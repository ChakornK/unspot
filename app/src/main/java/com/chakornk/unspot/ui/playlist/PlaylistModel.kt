package com.chakornk.unspot.ui.playlist

import org.json.JSONObject

data class Playlist(
	val uri: String,
	val cover: String,
	val title: String,
	val description: String,
	val duration: Long,
	val length: Int,
	val collaborators: List<Collaborator>
)

data class Collaborator(
	val name: String,
	val uri: String,
	val image: String
)

data class PlaylistContent(
	val items: List<PlaylistItem>,
	val offset: Int,
	val limit: Int,
	val totalLength: Int
)

data class PlaylistItem(
	val uri: String,
	val type: String,
	val cover: String,
	val title: String,
	val subtitle: String,
	val duration: Long
)

class PlaylistModel {
	val getPlaylistMessage = "getPlaylist"
	val getPlaylistResponse = "getPlaylistResponse"
	val getPlaylistContentMessage = "getPlaylistContent"
	val getPlaylistContentResponse = "getPlaylistContentResponse"
	val playMessage = "play"

	fun parsePlaylist(data: JSONObject): Playlist {
		val collaboratorsArray = data.optJSONArray("collaborators")
		val collaborators = mutableListOf<Collaborator>()
		if (collaboratorsArray != null) {
			for (i in 0 until collaboratorsArray.length()) {
				val obj = collaboratorsArray.getJSONObject(i)
				collaborators.add(
					Collaborator(
						name = obj.optString("name"),
						uri = obj.optString("uri"),
						image = obj.optString("image")
					)
				)
			}
		}

		return Playlist(
			uri = data.optString("uri"),
			cover = data.optString("cover"),
			title = data.optString("title"),
			description = data.optString("description"),
			duration = data.optLong("duration"),
			length = data.optInt("length"),
			collaborators = collaborators
		)
	}

	fun parsePlaylistContent(data: JSONObject): PlaylistContent {
		val itemsArray = data.optJSONArray("items")
		val items = mutableListOf<PlaylistItem>()
		if (itemsArray != null) {
			for (i in 0 until itemsArray.length()) {
				val obj = itemsArray.getJSONObject(i)
				items.add(
					PlaylistItem(
						uri = obj.optString("uri"),
						type = obj.optString("type"),
						cover = obj.optString("cover"),
						title = obj.optString("title"),
						subtitle = obj.optString("subtitle"),
						duration = obj.optLong("duration")
					)
				)
			}
		}

		return PlaylistContent(
			items = items,
			offset = data.optInt("offset"),
			limit = data.optInt("limit"),
			totalLength = data.optInt("totalLength")
		)
	}
}
