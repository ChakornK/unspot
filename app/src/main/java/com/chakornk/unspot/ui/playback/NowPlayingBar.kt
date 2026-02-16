package com.chakornk.unspot.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chakornk.unspot.ui.theme.ThemePreview
import com.chakornk.unspot.ui.theme.UnspotTheme
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Pause
import com.composables.icons.materialsymbols.outlinedfilled.Play_arrow

@Composable
fun NowPlayingBar(
	state: PlaybackState, onTogglePlayback: () -> Unit, modifier: Modifier = Modifier
) {
	if (state.title.isEmpty()) return

	Surface(
		modifier = modifier
			.fillMaxWidth()
			.height(80.dp)
			.padding(8.dp),
		color = MaterialTheme.colorScheme.surfaceContainerHigh,
		shape = RoundedCornerShape(8.dp),
		tonalElevation = 8.dp
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)
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
	}
}

@ThemePreview
@Composable
fun NowPlayingBarPreview() {
	UnspotTheme {
		NowPlayingBar(
			state = PlaybackState(
				title = "Song Title",
				artist = "Artist Name",
				albumArt = "",
				isPlaying = false,
			), onTogglePlayback = {})
	}
}
