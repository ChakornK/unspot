package com.chakornk.unspot.ui.playback

import org.json.JSONObject

data class PlaybackState(
	val title: String = "",
	val artist: String = "",
	val albumArt: String = "",
	val isPlaying: Boolean = false,
	val currentTime: Long = 0L,
	val totalTime: Long = 0L,
)

class PlaybackModel {
	val playbackStateUpdateMessage = "playbackStateUpdate"
	val togglePlaybackMessage = "togglePlayback"
	val skipTrackMessage = "skipTrack"
	val previousTrackMessage = "previousTrack"

	fun parsePlaybackState(data: JSONObject): PlaybackState {
		return PlaybackState(
			title = data.optString("title", ""),
			artist = data.optString("artist", ""),
			albumArt = data.optString("albumArt", ""),
			isPlaying = data.optBoolean("isPlaying", false),
			currentTime = data.optLong("currentTime", 0L),
			totalTime = data.optLong("totalTime", 0L)
		)
	}
}
