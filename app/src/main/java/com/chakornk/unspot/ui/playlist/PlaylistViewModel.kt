package com.chakornk.unspot.ui.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel
import org.json.JSONObject

class PlaylistViewModel(private val model: PlaylistModel = PlaylistModel()) : BaseGeckoViewModel() {
	var playlist by mutableStateOf<Playlist?>(null)
		private set

	var playlistContent by mutableStateOf<PlaylistContent?>(null)
		private set

	var isLoading by mutableStateOf(false)
		private set

	private var currentUri: String? = null

	fun loadPlaylist(uri: String) {
		if (currentUri == uri) return
		currentUri = uri
		isLoading = true
		playlist = null
		playlistContent = null
		sendMessage(model.getPlaylistMessage, JSONObject().put("uri", uri))
		sendMessage(model.getPlaylistContentMessage, JSONObject().put("uri", uri).put("offset", 0))
	}

	fun loadMoreContent() {
		val uri = currentUri ?: return
		val content = playlistContent ?: return
		val total = playlist?.length ?: content.totalLength

		if (!isLoading && content.items.size < total) {
			isLoading = true
			sendMessage(model.getPlaylistContentMessage, JSONObject().apply {
				put("uri", uri)
				put("offset", content.items.size)
			})
		}
	}

	fun playTrack(trackUri: String) {
		sendMessage(model.playMessage, JSONObject().apply {
			put("contextUri", playlist!!.uri)
			put("trackUri", trackUri)
		})
	}

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			model.getPlaylistResponse -> {
				message.data?.let {
					playlist = model.parsePlaylist(it)
					if (playlistContent != null) isLoading = false
				}
			}

			model.getPlaylistContentResponse -> {
				message.data?.let {
					val newContent = model.parsePlaylistContent(it)
					val currentContent = playlistContent

					if (currentContent == null || newContent.offset == 0) {
						playlistContent = newContent
					} else {
						val mergedItems =
							(currentContent.items + newContent.items).distinctBy { item -> item.index }
								.sortedBy { item -> item.index }
						playlistContent = newContent.copy(items = mergedItems)
					}

					if (playlist != null) isLoading = false
				}
			}
		}
	}

	override fun onManagerAttached(manager: WebExtensionManager) {
		currentUri?.let { uri ->
			sendMessage(model.getPlaylistMessage, JSONObject().put("uri", uri))
			sendMessage(model.getPlaylistContentMessage, JSONObject().put("uri", uri).put("offset", 0))
		}
	}
}
