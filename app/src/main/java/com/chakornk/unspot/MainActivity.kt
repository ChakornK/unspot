package com.chakornk.unspot


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chakornk.unspot.ui.screens.HomeScreen
import com.chakornk.unspot.ui.screens.LibraryScreen
import com.chakornk.unspot.ui.screens.SearchScreen
import com.chakornk.unspot.ui.theme.UnspotTheme
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
	object Home : Screen("home", "Home", Icons.Rounded.Home)
	object Search : Screen("search", "Search", Icons.Rounded.Search)
	object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
}

@Composable
fun SpotifyWebView() {
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

			session.loadUri("https://open.spotify.com")
		}
	}, update = { /* session management if needed */ })
}

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			val navController = rememberNavController()
			val items = listOf(
				Screen.Home, Screen.Search, Screen.Library
			)

			UnspotTheme {
				Scaffold(
					modifier = Modifier.fillMaxSize(), bottomBar = {
						NavigationBar {
							val navBackStackEntry by navController.currentBackStackEntryAsState()
							val currentRoute = navBackStackEntry?.destination?.route

							items.forEach { screen ->
								NavigationBarItem(
									icon = { Icon(screen.icon, contentDescription = screen.label) },
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
					}) { innerPadding ->
					Box(modifier = Modifier.padding(innerPadding)) {
						Box(modifier = Modifier.alpha(0f)) {
							SpotifyWebView()
						}

						NavHost(
							navController = navController,
							startDestination = Screen.Home.route,
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
