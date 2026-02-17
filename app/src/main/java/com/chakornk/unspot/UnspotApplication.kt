package com.chakornk.unspot

import android.app.Application
import android.os.Build
import com.chakornk.unspot.gecko.WebExtensionManager
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class UnspotApplication : Application() {
	private var _geckoRuntime: GeckoRuntime? = null
	val geckoRuntime: GeckoRuntime
		get() = _geckoRuntime
			?: throw IllegalStateException("GeckoRuntime not initialized. Are you in the main process?")

	val webExtensionManager = WebExtensionManager()

	override fun onCreate() {
		super.onCreate()

		if (!isMainProcess()) {
			return
		}

		val runtimeSettings = GeckoRuntimeSettings.Builder().remoteDebuggingEnabled(true)
			.arguments(arrayOf("--start-debugger-server", "9222")).build()

		_geckoRuntime = GeckoRuntime.create(this, runtimeSettings)
	}

	private fun isMainProcess(): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			packageName == getProcessName()
		} else {
			true
		}
	}
}
