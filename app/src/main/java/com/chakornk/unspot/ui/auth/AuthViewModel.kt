package com.chakornk.unspot.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel
import org.json.JSONObject

class AuthViewModel(private val model: AuthModel = AuthModel()) : BaseGeckoViewModel() {
	var isLoggedIn by mutableStateOf(AuthStorage.isLoggedIn)
		private set

	var isCheckingAuth by mutableStateOf(false)
		private set

	override fun onManagerAttached(manager: WebExtensionManager) {
		if (manager.isConnected()) {
			checkAuthStatus()
		} else {
			manager.onPortConnected = { checkAuthStatus() }
		}
	}

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			"getIsSignedInResponse" -> {
				val newStatus = (message.rawMessage as? JSONObject)?.optBoolean("data") ?: false
				val wasLoggedIn = isLoggedIn
				if (newStatus != wasLoggedIn) {
					isLoggedIn = newStatus
					if (wasLoggedIn && !newStatus) {
						sendMessage(model.goToLoginMessage)
					}
				}
				AuthStorage.isLoggedIn = newStatus
			}
		}
	}

	fun checkAuthStatus() {
		sendMessage(model.getIsSignedInMessage)
	}

	fun login(email: String, password: String) {
		val data = JSONObject().apply {
			put("email", email)
			put("password", password)
		}

		sendMessage(model.loginMessage, data)
	}

	fun updateLoggedIn(loggedIn: Boolean) {
		if (loggedIn != isLoggedIn) {
			isLoggedIn = loggedIn
			AuthStorage.isLoggedIn = loggedIn
			if (!loggedIn) {
				sendMessage(model.goToLoginMessage)
			}
		}
	}
}



