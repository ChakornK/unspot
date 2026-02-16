package com.chakornk.unspot

import android.os.Bundle
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.chakornk.unspot.ui.theme.UnspotTheme
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController


@Composable
fun SpotifyWebView() {
	val context = LocalContext.current

	val runtimeSettings = GeckoRuntimeSettings.Builder()
		.remoteDebuggingEnabled(true) // this is the key
		.arguments(arrayOf("--start-debugger-server", "9222")) // force the port if needed
		.build()
	val sessionSettings = GeckoSessionSettings.Builder()
		.usePrivateMode(false)
		.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
		.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
		.userAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0")
		.useTrackingProtection(false)
		.suspendMediaWhenInactive(false)
		.build()

	val runtime = remember { GeckoRuntime.create(context, runtimeSettings) }
	val session = remember { GeckoSession(sessionSettings) }

	// fix service worker glitch
	runtime.storageController.clearData(
		StorageController.ClearFlags.DOM_STORAGES
	)

	AndroidView(
		modifier = Modifier.fillMaxSize(),
		factory = { ctx ->
			GeckoView(ctx).apply {
				session.open(runtime)
				this.setSession(session)

				session.permissionDelegate = object : GeckoSession.PermissionDelegate {
					override fun onContentPermissionRequest(
						session: GeckoSession,
						perm: GeckoSession.PermissionDelegate.ContentPermission
					): GeckoResult<Int>? {
						return if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS) {
							GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
						} else null
					}
				}

				session.loadUri("https://open.spotify.com")
			}
		},
		update = { /* session management if needed */ }
	)
}

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			var selectedItem by remember { mutableStateOf(0) }

			UnspotTheme {
				Scaffold(
					modifier = Modifier.fillMaxSize(),
					bottomBar = {
						NavigationBar() {
							val navItems = listOf(
								"Home",
								"Search",
								"Library"
							)

							navItems.forEachIndexed { index, item ->
								NavigationBarItem(
									icon = {
										when (index) {
											0 -> Icon(Icons.Rounded.Home, contentDescription = item)
											1 -> Icon(Icons.Rounded.Search, contentDescription = item)
											2 -> Icon(Icons.Rounded.LibraryMusic, contentDescription = item)
											else -> Icon(Icons.Rounded.Home, contentDescription = item)
										}
									},
									label = { Text(item) },
									selected = selectedItem == index,
									onClick = { selectedItem = index }
								)
							}
						}
					}
				) { innerPadding ->
					Box(modifier = Modifier.padding(innerPadding)) {
						SpotifyWebView()
					}
				}
			}
		}
	}
}
