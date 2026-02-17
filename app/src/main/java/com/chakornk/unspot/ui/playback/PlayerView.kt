@file:OptIn(ExperimentalMaterial3Api::class)

package com.chakornk.unspot.ui.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.composables.icons.materialsymbols.outlinedfilled.Skip_next
import com.composables.icons.materialsymbols.outlinedfilled.Skip_previous
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun PlayerView(
	state: PlaybackState,
	isExpanded: Boolean,
	onToggleExpanded: () -> Unit,
	onTogglePlayback: () -> Unit,
	onSkipTrack: () -> Unit,
	onPreviousTrack: () -> Unit,
) {
	if (state.title.isEmpty()) return

	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	NowPlayingBar(
		state = state,
		onTogglePlayback = onTogglePlayback,
		onClick = onToggleExpanded,
		onSkipTrack = onSkipTrack,
		onPreviousTrack = onPreviousTrack,
	)

	if (isExpanded) {
		FullPlayer(
			state = state,
			onCollapse = onToggleExpanded,
			onTogglePlayback = onTogglePlayback,
			onSkipTrack = onSkipTrack,
			onPreviousTrack = onPreviousTrack,
			sheetState = sheetState
		)
	}
}

@Composable
private fun Modifier.swipeToSkip(
	onSkip: () -> Unit, onPrevious: () -> Unit, onOffsetChange: (Float) -> Unit
): Modifier {
	val haptic = LocalHapticFeedback.current
	val currentOnSkip by rememberUpdatedState(onSkip)
	val currentOnPrevious by rememberUpdatedState(onPrevious)
	val threshold = with(LocalDensity.current) { 80.dp.toPx() }
	var totalDrag by remember { mutableFloatStateOf(0f) }
	var hasVibrated by remember { mutableStateOf(false) }

	return this.pointerInput(Unit) {
		detectHorizontalDragGestures(onDragEnd = {
			if (totalDrag > threshold) {
				currentOnPrevious()
			} else if (totalDrag < -threshold) {
				currentOnSkip()
			}
			totalDrag = 0f
			onOffsetChange(0f)
			hasVibrated = false
		}, onDragCancel = {
			totalDrag = 0f
			onOffsetChange(0f)
			hasVibrated = false
		}, onHorizontalDrag = { change, dragAmount ->
			change.consume()
			totalDrag += dragAmount
			onOffsetChange(totalDrag)

			if (totalDrag.absoluteValue > threshold) {
				if (!hasVibrated) {
					haptic.performHapticFeedback(HapticFeedbackType.LongPress)
					hasVibrated = true
				}
			} else {
				hasVibrated = false
			}
		})
	}
}

@Composable
private fun SwipeIndicator(
	offset: Float, threshold: Float, modifier: Modifier = Modifier, isFullPlayer: Boolean = false
) {
	val isSkip = offset < 0
	val progress = (offset.absoluteValue / threshold).coerceIn(0f, 1.2f)
	val icon =
		if (isSkip) MaterialSymbols.OutlinedFilled.Skip_next else MaterialSymbols.OutlinedFilled.Skip_previous
	val isReached = offset.absoluteValue > threshold

	val backgroundColor by animateColorAsState(
		targetValue = if (isReached) MaterialTheme.colorScheme.secondaryContainer
		else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f + 0.7f * progress),
		label = "backgroundColor"
	)
	val contentColor by animateColorAsState(
		targetValue = if (isReached) MaterialTheme.colorScheme.onSecondaryContainer
		else MaterialTheme.colorScheme.onSurfaceVariant, label = "contentColor"
	)

	Box(
		modifier = modifier
			.fillMaxHeight()
			.alpha(progress),
		contentAlignment = if (isSkip) Alignment.CenterEnd else Alignment.CenterStart
	) {
		if (!isFullPlayer) {
			Box(
				modifier = Modifier
					.requiredSize((140 + (offset.absoluteValue / 4f)).dp)
					.offset(x = ((if (isSkip) 1 else -1) * (80 + (64 * (1 - progress)) + (offset.absoluteValue / 8f))).dp)
					.background(
						color = backgroundColor, shape = CircleShape
					)
					.align(Alignment.Center), contentAlignment = Alignment.Center
			) {}
		}
		Box(
			modifier = modifier
				.fillMaxHeight()
				.width(64.dp)
				.offset(x = ((if (isSkip) 1 else -1) * 64 * (1 - progress)).dp),
			contentAlignment = Alignment.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.size((24 + 8 * progress).dp),
				tint = contentColor
			)
		}
	}
}

@Composable
private fun FullPlayer(
	state: PlaybackState,
	onCollapse: () -> Unit,
	onTogglePlayback: () -> Unit,
	onSkipTrack: () -> Unit,
	onPreviousTrack: () -> Unit,
	sheetState: SheetState
) {
	var swipeOffsetTarget by remember { mutableFloatStateOf(0f) }
	val swipeOffset by animateFloatAsState(
		targetValue = swipeOffsetTarget,
		animationSpec = if (swipeOffsetTarget == 0f) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
		label = "swipeOffset"
	)
	val threshold = with(LocalDensity.current) { 80.dp.toPx() }

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

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.swipeToSkip(
						onSkip = onSkipTrack, onPrevious = onPreviousTrack
					) { swipeOffsetTarget = it }) {
				if (swipeOffset.absoluteValue > 0.1f) {
					if (swipeOffset > 0) {
						SwipeIndicator(
							offset = swipeOffset,
							threshold = threshold,
							modifier = Modifier.align(Alignment.CenterStart),
							isFullPlayer = true
						)
					} else if (swipeOffset < 0) {
						SwipeIndicator(
							offset = swipeOffset,
							threshold = threshold,
							modifier = Modifier.align(Alignment.CenterEnd),
							isFullPlayer = true
						)
					}
				}
				Box(modifier = Modifier.offset { IntOffset(swipeOffset.roundToInt(), 0) }) {
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
				}
			}

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
					value = if (state.totalTime > 0) state.currentTime / state.totalTime.toFloat() else 0f,
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
	onSkipTrack: () -> Unit,
	onPreviousTrack: () -> Unit,
	onClick: () -> Unit,
) {
	var swipeOffsetTarget by remember { mutableFloatStateOf(0f) }
	val swipeOffset by animateFloatAsState(
		targetValue = swipeOffsetTarget,
		animationSpec = if (swipeOffsetTarget == 0f) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
		label = "swipeOffset"
	)
	val threshold = with(LocalDensity.current) { 80.dp.toPx() }

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.height(80.dp)
			.padding(8.dp)
			.clip(RoundedCornerShape(8.dp))
			.swipeToSkip(
				onSkip = onSkipTrack, onPrevious = onPreviousTrack
			) { swipeOffsetTarget = it }
			.clickable { onClick() },
		color = MaterialTheme.colorScheme.surfaceContainerHigh,
		tonalElevation = 8.dp
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			if (swipeOffset.absoluteValue > 0.1f) {
				if (swipeOffset > 0) {
					SwipeIndicator(
						offset = swipeOffset,
						threshold = threshold,
						modifier = Modifier.align(Alignment.CenterStart),
						isFullPlayer = false
					)
				} else if (swipeOffset < 0) {
					SwipeIndicator(
						offset = swipeOffset,
						threshold = threshold,
						modifier = Modifier.align(Alignment.CenterEnd),
						isFullPlayer = false
					)
				}
			}

			Column(
				modifier = Modifier
					.fillMaxSize()
					.alpha(1 - (swipeOffset.absoluteValue / threshold).coerceIn(0f, 1f))
					.offset { IntOffset(swipeOffset.roundToInt(), 0) }) {
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
					progress = { if (state.totalTime > 0) state.currentTime / state.totalTime.toFloat() else 0f },
					modifier = Modifier
						.fillMaxWidth()
						.height(2.dp),
					trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
				)
			}
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
			onSkipTrack = {},
			onPreviousTrack = {},
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
		),
			onToggleExpanded = {},
			onTogglePlayback = {},
			onSkipTrack = {},
			onPreviousTrack = {},
			isExpanded = false
		)
	}
}
