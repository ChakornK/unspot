package com.chakornk.unspot.ui.library

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel

class LibraryViewModel(private val model: LibraryModel = LibraryModel()) : BaseGeckoViewModel() {
	var libraryItems by mutableStateOf<List<LibraryItem>>(emptyList())
		private set

	var searchQuery by mutableStateOf("")
	var isSearchActive by mutableStateOf(false)

	val filteredItems by derivedStateOf {
		if (searchQuery.isBlank()) {
			libraryItems
		} else {
			libraryItems.filter {
				it.title.contains(searchQuery, ignoreCase = true) ||
						it.subtitle.contains(searchQuery, ignoreCase = true)
			}
		}
	}

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			model.libraryUpdateMessage -> {
				message.data?.optJSONArray("items")?.let { items ->
					libraryItems = model.parseLibraryItems(items)
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

	fun toggleSearch() {
		isSearchActive = !isSearchActive
		if (!isSearchActive) {
			searchQuery = ""
		}
	}
}
