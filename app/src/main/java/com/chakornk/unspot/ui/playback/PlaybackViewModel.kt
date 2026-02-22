package com.chakornk.unspot.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chakornk.unspot.gecko.WebExtensionManager.WebExtensionMessage
import com.chakornk.unspot.ui.base.BaseGeckoViewModel

class PlaybackViewModel(private val model: PlaybackModel = PlaybackModel()) : BaseGeckoViewModel() {
	var playbackState by mutableStateOf(PlaybackState())
		private set

	override fun handleMessage(message: WebExtensionMessage) {
		when (message.type) {
			model.playbackStateUpdateMessage -> {
				message.data?.let {
					playbackState = model.parsePlaybackState(it)
				}
			}
		}
	}

	fun pausePlayback() {
		sendMessage(model.pausePlaybackMessage)
	}

	fun resumePlayback() {
		sendMessage(model.resumePlaybackMessage)
	}

	fun skipTrack() {
		sendMessage(model.skipTrackMessage)
	}

	fun previousTrack() {
		sendMessage(model.previousTrackMessage)
	}

	fun seekTo(position: Long) {
		sendMessage(model.setPlaybackPositionMessage, model.createSetPlaybackPositionMessage(position))
	}
}
