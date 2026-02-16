package com.chakornk.unspot.ui.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseGeckoViewModel : ViewModel() {
	private var webExtensionManager: WebExtensionManager? = null
	private var messageCollectionJob: Job? = null

	/**
	 * Attaches the WebExtensionManager to this ViewModel.
	 */
	fun attachManager(manager: WebExtensionManager) {
		if (webExtensionManager === manager && messageCollectionJob?.isActive == true) {
			return
		}

		messageCollectionJob?.cancel()

		webExtensionManager = manager

		messageCollectionJob = viewModelScope.launch {
			manager.messages.collect { message ->
				handleMessage(message)
			}
		}

		onManagerAttached(manager)
	}

	/**
	 * Override to handle incoming messages from the WebExtension.
	 */
	protected open fun handleMessage(message: WebExtensionMessage) {
	}

	/**
	 * Called when a manager is successfully attached.
	 */
	protected open fun onManagerAttached(manager: WebExtensionManager) {
	}

	/**
	 * Sends a message to the WebExtension.
	 */
	protected fun sendMessage(type: String, data: Any? = null) {
		webExtensionManager?.sendMessage(type, data) ?: Log.w(
			TAG, "Cannot send message '$type': Manager not attached"
		)
	}

	companion object {
		private const val TAG = "BaseGeckoViewModel"
	}
}
