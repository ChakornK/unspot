package com.chakornk.unspot.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.chakornk.unspot.MainActivity
import com.chakornk.unspot.UnspotApplication
import com.chakornk.unspot.ui.playback.PlaybackModel
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaPlaybackService : MediaSessionService() {
	private val customCommandShuffle = SessionCommand("ACTION_SHUFFLE", Bundle.EMPTY)
	private lateinit var mediaSession: MediaSession
	private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

	@UnstableApi
	override fun onCreate() {
		super.onCreate()
		setMediaNotificationProvider(
			DefaultMediaNotificationProvider(this)
		)

		val shuffleButton =
			CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF).setDisplayName("Shuffle")
				.setSessionCommand(customCommandShuffle).build()

		val app = application as UnspotApplication
		val webExtensionManager = app.webExtensionManager
		val playbackModel = PlaybackModel()
		val geckoPlayer = GeckoPlayer()
		val player = MediaCommandHandler(geckoPlayer, webExtensionManager, playbackModel)

		serviceScope.launch {
			webExtensionManager.messages.collect { message ->
				if (message.type == playbackModel.playbackStateUpdateMessage) {
					message.data?.let { data ->
						val state = playbackModel.parsePlaybackState(data)
						geckoPlayer.updateMediaItem(
							state.isPlaying,
							state.title,
							state.artist,
							state.albumArt,
							state.currentTime,
							state.totalTime
						)
					}
				}
			}
		}

		val intent = Intent(this, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

		mediaSession =
			MediaSession.Builder(this, player).setSessionActivity(pendingIntent).setId("unspot")
				.setCallback(MyCallback()).setMediaButtonPreferences(ImmutableList.of(shuffleButton))
				.build()
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
		return mediaSession
	}

	override fun onDestroy() {
		mediaSession.release()
		serviceScope.cancel()
		super.onDestroy()
	}

	private inner class MyCallback : MediaSession.Callback {
		@UnstableApi
		override fun onConnect(
			session: MediaSession, controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
				.setAvailableSessionCommands(
					MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
						.add(customCommandShuffle).build()
				).build()
		}

		override fun onCustomCommand(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			customCommand: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			if (customCommand.customAction == "ACTION_SHUFFLE") {
				return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
			}
			return super.onCustomCommand(session, controller, customCommand, args)
		}
	}
}
