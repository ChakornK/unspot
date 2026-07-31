package com.chakornk.unspot

import android.app.Application
import android.os.Build
import android.util.Log
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.ui.auth.AuthStorage
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension

class UnspotApplication : Application() {
	private var _geckoRuntime: GeckoRuntime? = null
	val geckoRuntime: GeckoRuntime
		get() = _geckoRuntime
			?: throw IllegalStateException("GeckoRuntime not initialized. Are you in the main process?")

	val webExtensionManager = WebExtensionManager()

	// ponytail: GeckoSession owned by Application so it outlives the Activity.
	// The foreground MediaPlaybackService keeps the process alive while playing,
	// so the session (and Spotify audio) survives the Activity being killed in the background.
	private var _geckoSession: GeckoSession? = null
	val geckoSession: GeckoSession
		get() = _geckoSession ?: throw IllegalStateException("GeckoSession not initialized")

	override fun onCreate() {
		super.onCreate()

		if (!isMainProcess()) {
			return
		}

		AuthStorage.init(this)

		val runtimeSettings = GeckoRuntimeSettings.Builder().remoteDebuggingEnabled(true)
			.arguments(arrayOf("--start-debugger-server", "9222")).build()

		_geckoRuntime = GeckoRuntime.create(this, runtimeSettings)
		createGeckoSession()
	}

	private fun createGeckoSession() {
		val runtime = _geckoRuntime!!
		val geckoVersion = org.mozilla.geckoview.BuildConfig.MOZILLA_VERSION.split(".")[0]

		val session = GeckoSession(
			GeckoSessionSettings.Builder().usePrivateMode(false)
				.viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
				.userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
				.userAgentOverride("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:$geckoVersion.0) Gecko/20100101 Firefox/$geckoVersion.0")
				.useTrackingProtection(false).suspendMediaWhenInactive(false).build()
		)

		session.permissionDelegate = object : GeckoSession.PermissionDelegate {
			override fun onContentPermissionRequest(
				session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission
			): GeckoResult<Int>? {
				return if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS || perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
					GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
				} else null
			}
		}

		runtime.webExtensionController.ensureBuiltIn(
			"resource://android/assets/unspot/", "@unspot"
		).accept({ extension: WebExtension? ->
			Log.i("MessageDelegate", "Extension installed: $extension")
			extension?.let {
				session.webExtensionController.setMessageDelegate(
					it, webExtensionManager.messageDelegate, "browser"
				)
			}
		}, { e: Throwable? ->
			Log.e("MessageDelegate", "Error registering WebExtension", e)
		})

		session.open(runtime)
		session.loadUri("https://open.spotify.com")
		_geckoSession = session
	}

	private fun isMainProcess(): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			packageName == getProcessName()
		} else {
			true
		}
	}
}
