package com.chakornk.unspot.playback

import android.os.Looper
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

@UnstableApi
class GeckoPlayer : SimpleBasePlayer(Looper.getMainLooper()) {
	private var isCurrentlyPlaying = false
	private var currentPosition = 0L

	private var playlist = listOf<MediaItemData>()

	override fun getState(): State {
		return State.Builder().setAvailableCommands(
			Player.Commands.Builder().addAll(
				COMMAND_PLAY_PAUSE,
				COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
				COMMAND_SEEK_TO_NEXT,
				COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
				COMMAND_SEEK_TO_PREVIOUS,
				COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
				COMMAND_GET_CURRENT_MEDIA_ITEM,
				COMMAND_GET_METADATA
			).build()
		).setPlaylist(playlist).setPlayWhenReady(
			isCurrentlyPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
		).setPlaybackState(
			if (playlist.isNotEmpty()) STATE_READY else STATE_IDLE
		).setContentPositionMs(currentPosition).setAudioAttributes(
			AudioAttributes.Builder().setContentType(AUDIO_CONTENT_TYPE_MUSIC).setUsage(USAGE_MEDIA)
				.build()
		).build()
	}

	fun updateMediaItem(
		isPlaying: Boolean,
		title: String,
		artist: String,
		albumArt: String,
		currentTime: Long,
		totalTime: Long
	) {
		val id = "$title-$artist-$albumArt";
		if (currentMediaItem?.mediaId != id) {
			Log.d("GeckoPlayer", "Updating media item from ${currentMediaItem?.mediaId} to $id")
			val mediaMetadata =
				MediaMetadata.Builder().setTitle(title).setArtist(artist).setArtworkUri(albumArt.toUri())
					.setDurationMs(totalTime).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()
			val mediaItem = MediaItem.Builder().setMediaId(id).setMediaMetadata(mediaMetadata).build()

			this.playlist = listOf(
				MediaItemData.Builder(mediaItem.mediaId!!).setMediaItem(mediaItem).build()
			)
		}
		isCurrentlyPlaying = isPlaying
		currentPosition = currentTime
		invalidateState()
	}
}
