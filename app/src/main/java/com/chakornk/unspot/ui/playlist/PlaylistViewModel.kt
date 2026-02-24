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
		sendMessage(model.getPlaylistMessage, JSONObject().put("uri", uri))
		sendMessage(model.getPlaylistContentMessage, JSONObject().put("uri", uri).put("offset", 0))
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
					playlistContent = model.parsePlaylistContent(it)
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
