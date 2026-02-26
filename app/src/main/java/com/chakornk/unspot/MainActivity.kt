package com.chakornk.unspot

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.playback.MediaPlaybackService
import com.chakornk.unspot.ui.auth.AuthViewModel
import com.chakornk.unspot.ui.auth.LoginScreen
import com.chakornk.unspot.ui.components.LoadingScreen
import com.chakornk.unspot.ui.home.HomeScreen
import com.chakornk.unspot.ui.library.LibraryScreen
import com.chakornk.unspot.ui.library.LibraryViewModel
import com.chakornk.unspot.ui.navigation.Tab
import com.chakornk.unspot.ui.navigation.View
import com.chakornk.unspot.ui.playback.PlaybackViewModel
import com.chakornk.unspot.ui.playback.PlayerView
import com.chakornk.unspot.ui.playlist.PlaylistScreen
import com.chakornk.unspot.ui.playlist.PlaylistViewModel
import com.chakornk.unspot.ui.search.SearchScreen
import com.chakornk.unspot.ui.theme.UnspotTheme
import com.chakornk.unspot.ui.welcome.WelcomeScreen
import com.chakornk.unspot.ui.welcome.WelcomeViewModel
import com.google.common.util.concurrent.MoreExecutors
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension

@Composable
fun SpotifyWebView(
	runtime: GeckoRuntime, authViewModel: AuthViewModel, webExtensionManager: WebExtensionManager
) {
	val sessionSettings = GeckoSessionSettings.Builder().usePrivateMode(false)
		.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
		.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
		.userAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:147.0) Gecko/20100101 Firefox/147.0")
		.useTrackingProtection(false).suspendMediaWhenInactive(false).build()

	val session = remember { GeckoSession(sessionSettings) }

	// fix service worker glitch
	runtime.storageController.clearData(
		StorageController.ClearFlags.DOM_STORAGES
	)
	runtime.webExtensionController.ensureBuiltIn(
		"resource://android/assets/unspot/", "@unspot"
	).accept({ extension: WebExtension? ->
		Log.i(
			"MessageDelegate", "Extension installed: $extension"
		)
		extension?.let {
			session.webExtensionController.setMessageDelegate(
				it, webExtensionManager.messageDelegate, "browser"
			)
		}
	}, { e: Throwable? ->
		Log.e(
			"MessageDelegate", "Error registering WebExtension", e
		)
	})

	AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
		GeckoView(ctx).apply {
			session.open(runtime)
			this.setSession(session)

			session.permissionDelegate = object : GeckoSession.PermissionDelegate {
				override fun onContentPermissionRequest(
					session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission
				): GeckoResult<Int>? {
					return if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS || perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
						GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
					} else null
				}
			}

			session.progressDelegate = object : GeckoSession.ProgressDelegate {
				override fun onPageStop(session: GeckoSession, success: Boolean) {
					authViewModel.checkAuthStatus()
				}
			}

			session.loadUri("https://open.spotify.com")
		}
	}, update = { /* session management if needed */ })
}

class MainActivity : ComponentActivity() {
	@OptIn(ExperimentalSharedTransitionApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val app = application as UnspotApplication
		val geckoRuntime = app.geckoRuntime
		val webExtensionManager = app.webExtensionManager

		val sessionToken = SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
		val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
		controllerFuture.addListener(
			{
				// controllerFuture.get()
			}, MoreExecutors.directExecutor()
		)

		requestNotificationPermission()

		setContent {
			val navController = rememberNavController()
			val authViewModel: AuthViewModel = viewModel()
			val welcomeViewModel: WelcomeViewModel = viewModel()
			val playbackViewModel: PlaybackViewModel = viewModel()

			authViewModel.attachManager(webExtensionManager)
			playbackViewModel.attachManager(webExtensionManager)

			val navBackStackEntry by navController.currentBackStackEntryAsState()
			val currentRoute = navBackStackEntry?.destination?.route

			val isLoggedIn = authViewModel.isLoggedIn
			val isCheckingAuth = authViewModel.isCheckingAuth

			var isPlayerExpanded by remember { mutableStateOf(false) }

			val tabs = listOf(
				Tab.Home, Tab.Search, Tab.Library
			)

			val context = LocalContext.current

			UnspotTheme {
				Box(modifier = Modifier.fillMaxSize()) {
					Box(modifier = Modifier.alpha(0f)) {
						SpotifyWebView(geckoRuntime, authViewModel, webExtensionManager)
					}

					if (isCheckingAuth) {
						Scaffold { innerPadding ->
							Box(modifier = Modifier.padding(innerPadding)) { LoadingScreen() }
						}
					} else {
						SharedTransitionLayout {
							Scaffold(
								modifier = Modifier.fillMaxSize(),
								contentWindowInsets = WindowInsets(),
								bottomBar = {
									if (isLoggedIn) {
										Column {
											PlayerView(
												state = playbackViewModel.playbackState,
												onResumePlayback = { playbackViewModel.resumePlayback() },
												onPausePlayback = { playbackViewModel.pausePlayback() },
												onToggleExpanded = { isPlayerExpanded = !isPlayerExpanded },
												onSkipTrack = { playbackViewModel.skipTrack() },
												onPreviousTrack = { playbackViewModel.previousTrack() },
												onSeek = { playbackViewModel.seekTo(it) },
												isExpanded = isPlayerExpanded
											)
											NavigationBar {
												tabs.forEach { tab ->
													NavigationBarItem(
														icon = {
														Icon(
															if (currentRoute == tab.route) tab.iconSelected else tab.icon,
															tab.label
														)
													},
														label = { Text(tab.label) },
														selected = currentRoute == tab.route,
														onClick = {
															navController.navigate(tab.route) {
																popUpTo(navController.graph.startDestinationId) {
																	saveState = true
																}
																launchSingleTop = true
																restoreState = true
															}
														})
												}
											}
										}
									}
								}) { innerPadding ->
								NavHost(
									navController = navController,
									startDestination = if (isLoggedIn) View.Home.route else View.Welcome.route,
									modifier = Modifier.padding(innerPadding),
									enterTransition = {
										fadeIn(tween(300)) + scaleIn(
											initialScale = 0.92f, animationSpec = tween(300)
										)
									},
									exitTransition = {
										fadeOut(tween(90))
									},
									popEnterTransition = {
										fadeIn(tween(300)) + scaleIn(
											initialScale = 0.92f, animationSpec = tween(300)
										)
									},
									popExitTransition = {
										fadeOut(tween(90))
									}) {
									composable(View.Welcome.route) {
										LaunchedEffect(Unit) {
											welcomeViewModel.events.collect { event ->
												when (event) {
													is WelcomeViewModel.WelcomeEvent.NavigateToLogin -> {
														navController.navigate(event.route)
													}

													is WelcomeViewModel.WelcomeEvent.OpenSignUp -> {
														val intent = Intent(
															Intent.ACTION_VIEW, event.url.toUri()
														)
														context.startActivity(intent)
													}
												}
											}
										}
										WelcomeScreen(viewModel = welcomeViewModel)
									}
									composable(View.Login.route) {
										LoginScreen(viewModel = authViewModel)
									}
									composable(View.Home.route) {
										HomeScreen(
											sharedTransitionScope = this@SharedTransitionLayout,
											animatedVisibilityScope = this@composable
										)
									}
									composable(View.Search.route) {
										SearchScreen(
											sharedTransitionScope = this@SharedTransitionLayout,
											animatedVisibilityScope = this@composable
										)
									}
									composable(View.Library.route) {
										val libraryViewModel: LibraryViewModel = viewModel()
										LaunchedEffect(Unit) {
											libraryViewModel.attachManager(webExtensionManager)
										}
										LibraryScreen(
											viewModel = libraryViewModel,
											sharedTransitionScope = this@SharedTransitionLayout,
											animatedVisibilityScope = this@composable,
											onNavigateToPlaylist = { uri ->
												navController.navigate(View.Playlist.createRoute(uri))
											})
									}
									composable(
										View.Playlist.route,
										arguments = listOf(navArgument("uri") { type = NavType.StringType })
									) { backStackEntry ->
										val uri = backStackEntry.arguments?.getString("uri") ?: ""
										val playlistViewModel: PlaylistViewModel = viewModel()
										LaunchedEffect(Unit) {
											playlistViewModel.attachManager(webExtensionManager)
										}
										PlaylistScreen(
											uri = uri,
											viewModel = playlistViewModel,
											sharedTransitionScope = this@SharedTransitionLayout,
											animatedVisibilityScope = this@composable,
											onBack = { navController.popBackStack() })
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
	}

	private fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			when {
				ContextCompat.checkSelfPermission(
					this, Manifest.permission.POST_NOTIFICATIONS
				) == PackageManager.PERMISSION_GRANTED -> {
				}

				else -> {
					requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		}
	}
}
