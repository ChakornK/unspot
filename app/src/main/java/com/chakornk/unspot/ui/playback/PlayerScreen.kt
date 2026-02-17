@file:OptIn(ExperimentalMaterial3Api::class)

package com.chakornk.unspot.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview
import com.chakornk.unspot.ui.theme.UnspotTheme
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Keyboard_arrow_down
import com.composables.icons.materialsymbols.outlinedfilled.Pause
import com.composables.icons.materialsymbols.outlinedfilled.Pause_circle
import com.composables.icons.materialsymbols.outlinedfilled.Play_arrow
import com.composables.icons.materialsymbols.outlinedfilled.Play_circle

@Composable
fun PlayerView(
	state: PlaybackState,
	isExpanded: Boolean,
	onToggleExpanded: () -> Unit,
	onTogglePlayback: () -> Unit,
	modifier: Modifier = Modifier
) {
	if (state.title.isEmpty()) return

	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	NowPlayingBar(
		state = state,
		onTogglePlayback = onTogglePlayback,
		onClick = onToggleExpanded,
		modifier = modifier
	)

	if (isExpanded) {
		FullPlayer(
			state = state,
			onCollapse = onToggleExpanded,
			onTogglePlayback = onTogglePlayback,
			sheetState = sheetState
		)
	}
}

@Composable
private fun FullPlayer(
	state: PlaybackState, onCollapse: () -> Unit, onTogglePlayback: () -> Unit, sheetState: SheetState
) {
	ModalBottomSheet(
		onDismissRequest = onCollapse,
		sheetState = sheetState,
		dragHandle = null,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		modifier = Modifier.windowInsetsPadding(WindowInsets(top = 64.dp))
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 24.dp, vertical = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Row(
				modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
			) {
				IconButton(onClick = onCollapse) {
					Icon(
						imageVector = MaterialSymbols.OutlinedFilled.Keyboard_arrow_down,
						contentDescription = "Close"
					)
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			AsyncImage(
				model = state.albumArt,
				contentDescription = "Album Art",
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(1f)
					.clip(RoundedCornerShape(12.dp))
					.background(MaterialTheme.colorScheme.surfaceContainer),
				contentScale = ContentScale.Crop
			)

			Spacer(modifier = Modifier.height(48.dp))

			Text(
				text = state.title, style = MaterialTheme.typography.headlineMedium.copy(
					fontWeight = FontWeight.Bold
				), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
			)

			Text(
				text = state.artist,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)

			Spacer(modifier = Modifier.height(32.dp))

			Column(modifier = Modifier.fillMaxWidth()) {
				Slider(
					value = state.currentTime / state.totalTime.toFloat(),
					onValueChange = { },
					modifier = Modifier.fillMaxWidth()
				)
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 4.dp),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						text = formatTime(state.currentTime / 1000),
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Text(
						text = formatTime(state.totalTime / 1000),
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			Spacer(modifier = Modifier.height(32.dp))

			IconButton(
				onClick = onTogglePlayback, modifier = Modifier.size(80.dp)
			) {
				Icon(
					imageVector = if (state.isPlaying) {
						MaterialSymbols.OutlinedFilled.Pause_circle
					} else {
						MaterialSymbols.OutlinedFilled.Play_circle
					},
					contentDescription = if (state.isPlaying) "Pause" else "Play",
					modifier = Modifier.fillMaxSize(),
					tint = MaterialTheme.colorScheme.primary
				)
			}

			Spacer(modifier = Modifier.height(48.dp))
		}
	}
}

@Composable
private fun NowPlayingBar(
	state: PlaybackState,
	onTogglePlayback: () -> Unit,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier
			.fillMaxWidth()
			.height(80.dp)
			.padding(8.dp)
			.clickable { onClick() },
		color = MaterialTheme.colorScheme.surfaceContainerHigh,
		shape = RoundedCornerShape(8.dp),
		tonalElevation = 8.dp
	) {
		Column {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.weight(1f)
					.padding(horizontal = 8.dp)
			) {
				AsyncImage(
					model = state.albumArt,
					contentDescription = "Album Art",
					modifier = Modifier
						.size(48.dp)
						.clip(RoundedCornerShape(4.dp))
						.background(MaterialTheme.colorScheme.surfaceContainer),
					contentScale = ContentScale.Crop
				)

				Column(
					modifier = Modifier
						.weight(1f)
						.padding(horizontal = 12.dp)
				) {
					Text(
						text = state.title, style = MaterialTheme.typography.bodyMedium.copy(
							fontWeight = FontWeight.Bold, fontSize = 14.sp
						), maxLines = 1, overflow = TextOverflow.Ellipsis
					)
					Text(
						text = state.artist,
						style = MaterialTheme.typography.bodySmall.copy(
							fontSize = 12.sp
						),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				IconButton(onClick = onTogglePlayback) {
					Icon(
						imageVector = if (state.isPlaying) {
							MaterialSymbols.OutlinedFilled.Pause
						} else {
							MaterialSymbols.OutlinedFilled.Play_arrow
						}, contentDescription = if (state.isPlaying) "Pause" else "Play"
					)
				}
			}
			LinearProgressIndicator(
				progress = { state.currentTime / state.totalTime.toFloat() },
				modifier = Modifier
					.fillMaxWidth()
					.height(2.dp),
				trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
			)
		}
	}
}

private fun formatTime(seconds: Long): String {
	val minutes = seconds / 60
	val remainingSeconds = seconds % 60
	return "%d:%02d".format(minutes, remainingSeconds)
}

@ThemePreview
@Composable
fun FullPlayerPreview() {
	PreviewScreen(bottomBar = {
		FullPlayer(
			state = PlaybackState(
			title = "Song Title",
			artist = "Artist Name",
			albumArt = "",
			isPlaying = false,
			currentTime = 30000,
			totalTime = 210000,
		),
			onCollapse = {},
			onTogglePlayback = {},
			sheetState = SheetState(
				skipPartiallyExpanded = true,
				positionalThreshold = { 0f },
				velocityThreshold = { 0f })
		)
	}) {}
}

@ThemePreview
@Composable
fun PlayerViewPreview() {
	UnspotTheme {
		PlayerView(
			state = PlaybackState(
			title = "Song Title",
			artist = "Artist Name",
			albumArt = "",
			isPlaying = false,
			currentTime = 30000,
			totalTime = 210000,
		), onToggleExpanded = {}, onTogglePlayback = {}, isExpanded = false
		)
	}
}
