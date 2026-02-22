package com.chakornk.unspot.ui.library

import org.json.JSONArray

data class LibraryItem(
	val uri: String,
	val type: String,
	val cover: String,
	val title: String,
	val subtitle: String,
	val isActive: Boolean
)

class LibraryModel {
	val getLibraryDataMessage = "getLibraryData"
	val libraryUpdateMessage = "getLibraryDataResponse"

	fun parseLibraryItems(data: JSONArray): List<LibraryItem> {
		val items = mutableListOf<LibraryItem>()
		for (i in 0 until data.length()) {
			val obj = data.optJSONObject(i) ?: continue
			items.add(
				LibraryItem(
					uri = obj.optString("uri"),
					type = obj.optString("type"),
					cover = obj.optString("cover"),
					title = obj.optString("title"),
					subtitle = obj.optString("subtitle"),
					isActive = obj.optBoolean("isActive")
				)
			)
		}
		return items
	}
}
