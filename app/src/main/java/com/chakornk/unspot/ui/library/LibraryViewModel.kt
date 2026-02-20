package com.chakornk.unspot.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel

class LibraryViewModel(private val model: LibraryModel = LibraryModel()) : BaseGeckoViewModel() {
	var libraryItems by mutableStateOf<List<LibraryItem>>(emptyList())
		private set

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			model.libraryUpdateMessage -> {
				message.data?.optJSONArray("items")?.let { items ->
					libraryItems = model.parseLibraryItems(items).sortedBy { it.index }
				}
			}
		}
	}

	override fun onManagerAttached(manager: WebExtensionManager) {
		refreshLibrary()
	}

	fun refreshLibrary() {
		sendMessage(model.getLibraryDataMessage)
	}
}
