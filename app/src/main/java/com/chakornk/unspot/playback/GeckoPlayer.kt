package com.chakornk.unspot.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

@UnstableApi
class GeckoPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
	override fun getState(): State {
		return State.Builder().setAvailableCommands(
			Player.Commands.Builder().addAll(
				COMMAND_PLAY_PAUSE,
				COMMAND_SEEK_TO_NEXT,
				COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
				COMMAND_SEEK_TO_PREVIOUS,
				COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
				COMMAND_GET_CURRENT_MEDIA_ITEM,
				COMMAND_GET_METADATA
			).build()
		).setPlayWhenReady(true, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
			.setPlaybackState(STATE_IDLE).build()
	}
}
