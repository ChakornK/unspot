package com.chakornk.unspot.playback

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.chakornk.unspot.UnspotApplication
import com.chakornk.unspot.ui.playback.PlaybackModel
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MediaPlaybackService : MediaSessionService() {
	private val ACTION_FAVORITES = "ACTION_FAVORITES"
	private val customCommandFavorites = SessionCommand(ACTION_FAVORITES, Bundle.EMPTY)
	private var mediaSession: MediaSession? = null

	@UnstableApi
	override fun onCreate() {
		super.onCreate()
		val favoriteButton =
			CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED).setDisplayName("Save to favorites")
				.setSessionCommand(customCommandFavorites).build()

		val app = application as UnspotApplication
		val webExtensionManager = app.webExtensionManager
		val playbackModel = PlaybackModel()
		val geckoPlayer = GeckoPlayer()
		val player = MediaCommandHandler(geckoPlayer, webExtensionManager, playbackModel)

		// Build the session with a custom layout.
		mediaSession = MediaSession.Builder(this, player).setCallback(MyCallback())
			.setMediaButtonPreferences(ImmutableList.of(favoriteButton)).build()
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
		return mediaSession
	}

	private inner class MyCallback : MediaSession.Callback {
		@UnstableApi
		override fun onConnect(
			session: MediaSession, controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
				.setAvailableSessionCommands(
					MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
						.add(customCommandFavorites).build()
				).build()
		}

		override fun onCustomCommand(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			customCommand: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			if (customCommand.customAction == ACTION_FAVORITES) {
				// Do custom logic here
				return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
			}
			return super.onCustomCommand(session, controller, customCommand, args)
		}
	}
}
