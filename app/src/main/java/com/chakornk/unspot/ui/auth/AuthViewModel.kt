package com.chakornk.unspot.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel
import org.json.JSONObject

class AuthViewModel(private val model: AuthModel = AuthModel()) : BaseGeckoViewModel() {
	var isLoggedIn by mutableStateOf(false)
		private set

	var isCheckingAuth by mutableStateOf(true)
		private set

	override fun onManagerAttached(manager: WebExtensionManager) {
		checkAuthStatus()
	}

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			"getIsSignedInResponse" -> {
				isLoggedIn = (message.rawMessage as? JSONObject)?.optBoolean("data") ?: false
				isCheckingAuth = false

				if (!isLoggedIn) {
					sendMessage(model.goToLoginMessage)
				}
			}
		}
	}

	fun checkAuthStatus() {
		isCheckingAuth = true
		sendMessage(model.getIsSignedInMessage)
	}

	fun login(email: String, password: String) {
		val data = JSONObject().apply {
			put("email", email)
			put("password", password)
		}

		sendMessage(model.loginMessage, data)
	}
}



