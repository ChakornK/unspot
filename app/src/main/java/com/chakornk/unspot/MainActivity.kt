package com.chakornk.unspot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.ui.auth.AuthViewModel
import com.chakornk.unspot.ui.auth.LoginScreen
import com.chakornk.unspot.ui.components.LoadingScreen
import com.chakornk.unspot.ui.home.HomeScreen
import com.chakornk.unspot.ui.library.LibraryScreen
import com.chakornk.unspot.ui.navigation.Tab
import com.chakornk.unspot.ui.navigation.View
import com.chakornk.unspot.ui.playback.PlaybackViewModel
import com.chakornk.unspot.ui.playback.PlayerView
import com.chakornk.unspot.ui.search.SearchScreen
import com.chakornk.unspot.ui.theme.UnspotTheme
import com.chakornk.unspot.ui.welcome.WelcomeScreen
import com.chakornk.unspot.ui.welcome.WelcomeViewModel
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
		.userAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
		.useTrackingProtection(false).suspendMediaWhenInactive(false).build()

	val session = remember { GeckoSession(sessionSettings) }

	// fix service worker glitch
	runtime.storageController.clearData(
		StorageController.ClearFlags.DOM_STORAGES
	)
	runtime.webExtensionController.ensureBuiltIn("resource://android/assets/messaging/", "@unspot")
		.accept({ extension: WebExtension? ->
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
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val geckoRuntime = (application as UnspotApplication).geckoRuntime

		setContent {
			val navController = rememberNavController()
			val authViewModel: AuthViewModel = viewModel()
			val welcomeViewModel: WelcomeViewModel = viewModel()
			val playbackViewModel: PlaybackViewModel = viewModel()
			val webExtensionManager = remember { WebExtensionManager() }

			authViewModel.attachManager(webExtensionManager)
			playbackViewModel.attachManager(webExtensionManager)

			val isLoggedIn = authViewModel.isLoggedIn
			val isCheckingAuth = authViewModel.isCheckingAuth

			var isPlayerExpanded by remember { mutableStateOf(false) }

			val tabs = listOf(
				Tab.Home, Tab.Search, Tab.Library
			)

			val context = LocalContext.current

			UnspotTheme {
				Box(modifier = Modifier.fillMaxSize()) {
					// Always keep SpotifyWebView active in the background to handle page loading and auth checks
					Box(modifier = Modifier.alpha(0f)) {
						SpotifyWebView(geckoRuntime, authViewModel, webExtensionManager)
					}

					if (isCheckingAuth) {
						Scaffold { innerPadding ->
							Box(modifier = Modifier.padding(innerPadding)) { LoadingScreen() }
						}
					} else {
						Scaffold(
							modifier = Modifier.fillMaxSize(), bottomBar = {
								if (isLoggedIn) {
									Column {
										PlayerView(
											state = playbackViewModel.playbackState,
											onTogglePlayback = { playbackViewModel.togglePlayback() },
											onToggleExpanded = { isPlayerExpanded = !isPlayerExpanded },
											onSkipTrack = { playbackViewModel.skipTrack() },
											onPreviousTrack = { playbackViewModel.previousTrack() },
											isExpanded = isPlayerExpanded
										)
										NavigationBar {
											val navBackStackEntry by navController.currentBackStackEntryAsState()
											val currentRoute = navBackStackEntry?.destination?.route

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
							Box(modifier = Modifier.padding(innerPadding)) {
								NavHost(
									navController = navController,
									startDestination = if (isLoggedIn) View.Home.route else View.Welcome.route,
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
									composable(View.Home.route) { HomeScreen() }
									composable(View.Search.route) { SearchScreen() }
									composable(View.Library.route) { LibraryScreen() }
								}
							}
						}
					}

				}
			}
		}
	}
}
