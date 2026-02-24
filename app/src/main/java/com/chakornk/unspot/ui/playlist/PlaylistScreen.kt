package com.chakornk.unspot.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chakornk.unspot.ui.components.LoadingScreen
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Arrow_back

@Composable
fun PlaylistScreen(
	uri: String,
	viewModel: PlaylistViewModel,
	onBack: () -> Unit,
	onSetTopBar: (@Composable () -> Unit) -> Unit
) {
	val lazyListState = rememberLazyListState()
	val density = LocalDensity.current

	val minHeight = 64.dp
	val maxHeight = 320.dp

	val scrollOffset by remember {
		derivedStateOf {
			if (lazyListState.firstVisibleItemIndex > 0) 10000f
			else lazyListState.firstVisibleItemScrollOffset.toFloat()
		}
	}

	val collapseRange = with(density) { (maxHeight - minHeight).toPx() }
	val progress = (scrollOffset / collapseRange).coerceIn(0f, 1f)

	LaunchedEffect(uri) {
		viewModel.loadPlaylist(uri)
	}

	LaunchedEffect(Unit) {
		onSetTopBar { }
	}

	DisposableEffect(Unit) {
		onDispose {
			onSetTopBar {}
		}
	}

	val shouldLoadMore by remember {
		derivedStateOf {
			val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
			lastVisibleItem != null && lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 10
		}
	}

	LaunchedEffect(shouldLoadMore) {
		if (shouldLoadMore) {
			viewModel.loadMoreContent()
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		if (viewModel.isLoading && viewModel.playlist == null) {
			LoadingScreen()
		} else {
			val playlist = viewModel.playlist
			val content = viewModel.playlistContent

			LazyColumn(
				state = lazyListState, modifier = Modifier.fillMaxSize()
			) {
				item {
					Spacer(modifier = Modifier.height(maxHeight))
				}

				content?.items?.let { items ->
					items(items, key = { it.index }) { item ->
						PlaylistItemRow(item, onClick = {
							viewModel.playTrack(item.uri)
						})
					}
				}

				if (viewModel.isLoading) {
					val total = playlist?.length ?: content?.totalLength ?: 0
					val loadedCount = content?.items?.size ?: 0
					val remaining = total - loadedCount
					val placeholderCount = if (content == null) 10 else remaining

					items(placeholderCount) {
						PlaylistItemPlaceholder()
					}
				}
			}

			DynamicTopAppBar(
				playlist = playlist,
				progress = progress,
				onBack = onBack,
				maxHeight = maxHeight,
				minHeight = minHeight
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
	playlist: Playlist?, progress: Float, onBack: () -> Unit, maxHeight: Dp, minHeight: Dp
) {
	val density = LocalDensity.current
	val currentHeight = maxHeight - (maxHeight - minHeight) * progress

	Surface(
		modifier = Modifier
			.fillMaxWidth()
			.height(currentHeight),
		color = MaterialTheme.colorScheme.surface.copy(alpha = progress),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 8.dp)
		) {
			IconButton(
				onClick = onBack,
				modifier = Modifier
					.padding(start = 4.dp)
					.align(Alignment.TopStart)
					.size(48.dp),
				colors = IconButtonDefaults.iconButtonColors(
					containerColor = Color.Black.copy(alpha = 0.3f * (1f - progress)),
					contentColor = if (progress > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
				)
			) {
				Icon(MaterialSymbols.Outlined.Arrow_back, contentDescription = "Back")
			}

			if (playlist != null) {
				val imageProgress = (progress / 0.5f).coerceIn(0f, 1f)
				val imageInitialSize = 180.dp
				val imageFinalSize = 96.dp
				val imageSize = imageInitialSize - (imageInitialSize - imageFinalSize) * imageProgress

				val moveOutProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
				val imageAlpha = 1f - moveOutProgress
				val imageTranslationY = with(density) { (-100).dp.toPx() * moveOutProgress }

				AsyncImage(
					model = playlist.cover,
					contentDescription = null,
					modifier = Modifier
						.align(Alignment.TopCenter)
						.padding(top = 16.dp * (1f - progress))
						.graphicsLayer {
							translationY = imageTranslationY
						}
						.size(imageSize)
						.alpha(imageAlpha)
						.clip(RoundedCornerShape(8.dp))
						.background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
					contentScale = ContentScale.Crop)

				val titleX = 16.dp + (56.dp - 16.dp) * ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
				val titleY = (16.dp + 180.dp + 16.dp) * (1f - progress) + 8.dp * progress
				val titleSizeLarge = MaterialTheme.typography.headlineMedium.fontSize.value;
				val titleSizeSmall = MaterialTheme.typography.titleLarge.fontSize.value;
				val titleSize = titleSizeLarge - (titleSizeLarge - titleSizeSmall) * progress


				Text(
					text = playlist.title,
					style = MaterialTheme.typography.titleLarge.copy(fontSize = titleSize.sp),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.graphicsLayer {
						translationX = with(density) { titleX.toPx() }
						translationY = with(density) { titleY.toPx() }
					})

				val extraAlpha = (1f - progress * 2f).coerceIn(0f, 1f)
				Column(modifier = Modifier
					.padding(horizontal = 16.dp)
					.graphicsLayer {
						translationY =
							with(density) { (16.dp + 180.dp + 16.dp + 36.dp).toPx() * (1f - progress) + 24.dp.toPx() * progress }
					}
					.alpha(extraAlpha)) {
					Text(
						text = playlist.description.ifEmpty { "No description" },
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					Spacer(modifier = Modifier.size(4.dp))
					Text(
						text = "${playlist.length} songs • ${formatDuration(playlist.duration)}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
	}
}

@Composable
fun PlaylistItemRow(item: PlaylistItem, onClick: () -> Unit) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
	) {
		AsyncImage(
			model = item.cover,
			contentDescription = null,
			modifier = Modifier
				.size(48.dp)
				.clip(RoundedCornerShape(4.dp))
				.background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
			contentScale = ContentScale.Crop
		)

		Spacer(modifier = Modifier.width(16.dp))

		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = item.title,
				style = MaterialTheme.typography.bodyLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Text(
				text = item.subtitle,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
fun PlaylistItemPlaceholder() {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(48.dp)
				.clip(RoundedCornerShape(4.dp))
				.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
		)

		Spacer(modifier = Modifier.width(16.dp))

		Column(modifier = Modifier.weight(1f)) {
			Box(
				modifier = Modifier
					.width(150.dp)
					.height(16.dp)
					.clip(RoundedCornerShape(4.dp))
					.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
			)
			Spacer(modifier = Modifier.height(12.dp))
			Box(
				modifier = Modifier
					.width(100.dp)
					.height(12.dp)
					.clip(RoundedCornerShape(4.dp))
					.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
			)
		}
	}
}

private fun formatDuration(ms: Long): String {
	val totalSeconds = ms / 1000
	val minutes = totalSeconds / 60
	val hours = minutes / 60
	return if (hours > 0) {
		"${hours}h ${minutes % 60}m"
	} else {
		"${minutes}m ${totalSeconds % 60}s"
	}
}

@ThemePreview
@Composable
fun PlaylistScreenPreview() {
	PreviewScreen {
		val mockPlaylist = Playlist(
			uri = "spotify:playlist:1",
			cover = "https://example.com/cover.jpg",
			title = "My Awesome Playlist",
			description = "This is a great description for a playlist.",
			duration = 3600000,
			length = 20,
			collaborators = emptyList()
		)
		val mockItems = listOf(
			PlaylistItem(0, "uri1", "track", "https://example.com/1.jpg", "Track 1", "Artist 1", 180000),
			PlaylistItem(1, "uri2", "track", "https://example.com/2.jpg", "Track 2", "Artist 2", 210000),
			PlaylistItem(2, "uri3", "track", "https://example.com/3.jpg", "Track 3", "Artist 3", 150000),
		)

		Box(modifier = Modifier.fillMaxSize()) {
			LazyColumn(modifier = Modifier.fillMaxSize()) {
				item {
					Spacer(modifier = Modifier.height(312.dp))
				}
				items(mockItems) { item ->
					PlaylistItemRow(item, onClick = {})
				}
				items(3) {
					PlaylistItemPlaceholder()
				}
			}

			DynamicTopAppBar(
				playlist = mockPlaylist, progress = 0f, onBack = {}, maxHeight = 312.dp, minHeight = 64.dp
			)
		}
	}
}

@ThemePreview
@Composable
fun PlaylistScreenCollapsedPreview() {
	PreviewScreen {
		val mockPlaylist = Playlist(
			uri = "spotify:playlist:1",
			cover = "https://example.com/cover.jpg",
			title = "My Awesome Playlist",
			description = "This is a great description for a playlist.",
			duration = 3600000,
			length = 20,
			collaborators = emptyList()
		)
		val mockItems = listOf(
			PlaylistItem(0, "uri1", "track", "https://example.com/1.jpg", "Track 1", "Artist 1", 180000),
			PlaylistItem(1, "uri2", "track", "https://example.com/2.jpg", "Track 2", "Artist 2", 210000),
			PlaylistItem(2, "uri3", "track", "https://example.com/3.jpg", "Track 3", "Artist 3", 150000),
		)

		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
			LazyColumn(modifier = Modifier.fillMaxSize()) {
				item {
					Spacer(modifier = Modifier.height(64.dp))
				}
				items(mockItems) { item ->
					PlaylistItemRow(item, onClick = {})
				}
				items(3) {
					PlaylistItemPlaceholder()
				}
			}

			DynamicTopAppBar(
				playlist = mockPlaylist, progress = 1f, onBack = {}, maxHeight = 312.dp, minHeight = 64.dp
			)
		}
	}
}
