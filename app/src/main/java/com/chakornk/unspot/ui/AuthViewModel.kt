package com.chakornk.unspot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chakornk.unspot.gecko.WebExtensionManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel : ViewModel() {
	var isLoggedIn by mutableStateOf(false)
		private set

	var isCheckingAuth by mutableStateOf(true)
		private set

	private var webExtensionManager: WebExtensionManager? = null

	fun initialize(manager: WebExtensionManager) {
		if (this.webExtensionManager != null) return
		this.webExtensionManager = manager

		viewModelScope.launch {
			manager.messages.collect { message ->
				when (message.type) {
					"getIsSignedInResponse" -> {
						isLoggedIn =
							(message.rawMessage as? JSONObject)?.optBoolean("data") ?: false
						isCheckingAuth = false

						if (!isLoggedIn) {
							manager.sendMessage("goToLogin")
						}
					}
				}
			}
		}
	}

	fun checkAuthStatus() {
		val manager = webExtensionManager ?: return
		isCheckingAuth = true
		manager.sendMessage("getIsSignedIn")
	}

	fun login(email: String, password: String) {
		val manager = webExtensionManager ?: return

		val data = JSONObject().apply {
			put("email", email)
			put("password", password)
		}

		manager.sendMessage("login", data)
	}
}

