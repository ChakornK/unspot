package com.chakornk.unspot


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.ui.AuthViewModel
import com.chakornk.unspot.ui.screens.HomeScreen
import com.chakornk.unspot.ui.screens.LibraryScreen
import com.chakornk.unspot.ui.screens.LoginScreen
import com.chakornk.unspot.ui.screens.SearchScreen
import com.chakornk.unspot.ui.screens.WelcomeScreen
import com.chakornk.unspot.ui.theme.UnspotTheme
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Home
import com.composables.icons.materialsymbols.outlined.Library_music
import com.composables.icons.materialsymbols.outlined.Search
import com.composables.icons.materialsymbols.outlinedfilled.Home
import com.composables.icons.materialsymbols.outlinedfilled.Library_music
import com.composables.icons.materialsymbols.outlinedfilled.Search
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension


sealed class Screen(
	val route: String,
	val label: String,
	val icon: ImageVector,
	val iconSelected: ImageVector
) {
	object Welcome : Screen(
		"welcome",
		"Welcome",
		MaterialSymbols.Outlined.Home,
		MaterialSymbols.OutlinedFilled.Home
	)

	object Login :
		Screen("login", "Login", MaterialSymbols.Outlined.Home, MaterialSymbols.OutlinedFilled.Home)

	object Home :
		Screen("home", "Home", MaterialSymbols.Outlined.Home, MaterialSymbols.OutlinedFilled.Home)

	object Search : Screen(
		"search",
		"Search",
		MaterialSymbols.Outlined.Search,
		MaterialSymbols.OutlinedFilled.Search
	)

	object Library : Screen(
		"library",
		"Library",
		MaterialSymbols.Outlined.Library_music,
		MaterialSymbols.OutlinedFilled.Library_music
	)
}

@Composable
fun SpotifyWebView(authViewModel: AuthViewModel, webExtensionManager: WebExtensionManager) {
	val context = LocalContext.current

	val runtimeSettings =
		GeckoRuntimeSettings.Builder().remoteDebuggingEnabled(true)
			.arguments(arrayOf("--start-debugger-server", "9222"))
			.build()
	val sessionSettings = GeckoSessionSettings.Builder().usePrivateMode(false)
		.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
		.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
		.userAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
		.useTrackingProtection(false).suspendMediaWhenInactive(false).build()

	val runtime = remember { GeckoRuntime.create(context, runtimeSettings) }
	val session = remember { GeckoSession(sessionSettings) }

	// fix service worker glitch
	runtime.storageController.clearData(
		StorageController.ClearFlags.DOM_STORAGES
	)
	runtime.webExtensionController
		.ensureBuiltIn("resource://android/assets/messaging/", "@unspot")
		.accept(
			{ extension: WebExtension? ->
				Log.i(
					"MessageDelegate",
					"Extension installed: $extension"
				)
				extension?.let {
					session.webExtensionController.setMessageDelegate(
						it,
						webExtensionManager.messageDelegate,
						"browser"
					)
				}
			},
			{ e: Throwable? ->
				Log.e(
					"MessageDelegate",
					"Error registering WebExtension",
					e
				)
			}
		)

	AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
		GeckoView(ctx).apply {
			session.open(runtime)
			this.setSession(session)

			session.permissionDelegate = object : GeckoSession.PermissionDelegate {
				override fun onContentPermissionRequest(
					session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission
				): GeckoResult<Int>? {
					return if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS) {
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

		setContent {
			val navController = rememberNavController()
			val authViewModel: AuthViewModel = viewModel()
			val webExtensionManager = remember { WebExtensionManager() }

			authViewModel.initialize(webExtensionManager)

			val isLoggedIn = authViewModel.isLoggedIn
			val isCheckingAuth = authViewModel.isCheckingAuth

			val items = listOf(
				Screen.Home, Screen.Search, Screen.Library
			)

			val context = LocalContext.current

			UnspotTheme {
				Scaffold(
					modifier = Modifier.fillMaxSize(),
					bottomBar = {
						if (isLoggedIn && !isCheckingAuth) {
							NavigationBar {
								val navBackStackEntry by navController.currentBackStackEntryAsState()
								val currentRoute = navBackStackEntry?.destination?.route

								items.forEach { screen ->
									NavigationBarItem(
										icon = {
											Icon(
												if (currentRoute == screen.route) screen.iconSelected else screen.icon,
												screen.label
											)
										},
										label = { Text(screen.label) },
										selected = currentRoute == screen.route,
										onClick = {
											navController.navigate(screen.route) {
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
					}) { innerPadding ->
					Box(modifier = Modifier.padding(innerPadding)) {
						Box(modifier = Modifier.alpha(0f)) {
							SpotifyWebView(authViewModel, webExtensionManager)
						}

						NavHost(
							navController = navController,
							startDestination = if (isLoggedIn) Screen.Home.route else Screen.Welcome.route,
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
							composable(Screen.Welcome.route) {
								WelcomeScreen(onSignInClick = {
									navController.navigate(Screen.Login.route)
								}, onSignUpClick = {
									val intent =
										Intent(
											Intent.ACTION_VIEW,
											Uri.parse("https://www.spotify.com/signup")
										)
									context.startActivity(intent)
								})
							}
							composable(Screen.Login.route) {
								LoginScreen(onLoginClick = { email, password ->
									authViewModel.login(email, password)
								})
							}
							composable(Screen.Home.route) { HomeScreen() }
							composable(Screen.Search.route) { SearchScreen() }
							composable(Screen.Library.route) { LibraryScreen() }
						}
					}
				}
			}
		}
	}
}

