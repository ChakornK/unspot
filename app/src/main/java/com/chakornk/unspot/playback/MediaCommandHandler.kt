package com.chakornk.unspot.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.chakornk.unspot.gecko.WebExtensionManager
import com.chakornk.unspot.ui.playback.PlaybackModel

@UnstableApi
class MediaCommandHandler(
	player: Player,
	private val webExtensionManager: WebExtensionManager,
	private val playbackModel: PlaybackModel
) : ForwardingPlayer(player) {

	override fun play() {
		webExtensionManager.sendMessage(playbackModel.togglePlaybackMessage)
	}

	override fun pause() {
		webExtensionManager.sendMessage(playbackModel.togglePlaybackMessage)
	}

	override fun seekToNext() {
		webExtensionManager.sendMessage(playbackModel.skipTrackMessage)
	}

	override fun seekToPrevious() {
		webExtensionManager.sendMessage(playbackModel.previousTrackMessage)
	}
}
