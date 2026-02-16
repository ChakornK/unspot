package com.chakornk.unspot.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel
import org.json.JSONObject

data class PlaybackState(
	val title: String = "",
	val artist: String = "",
	val albumArt: String = "",
	val isPlaying: Boolean = false
)

class PlaybackViewModel(private val model: PlaybackModel = PlaybackModel()) : BaseGeckoViewModel() {
	var playbackState by mutableStateOf(PlaybackState())
		private set

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			model.playbackStateUpdateMessage -> {
				message.data?.let { updatePlaybackState(it) }
			}
		}
	}

	private fun updatePlaybackState(data: JSONObject) {
		playbackState = PlaybackState(
			title = data.optString("title", ""),
			artist = data.optString("artist", ""),
			albumArt = data.optString("albumArt", ""),
			isPlaying = data.optBoolean("isPlaying", false)
		)
	}

	fun togglePlayback() {
		sendMessage(model.togglePlaybackMessage)
	}
}
