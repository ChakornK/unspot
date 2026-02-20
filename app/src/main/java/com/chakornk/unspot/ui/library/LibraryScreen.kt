package com.chakornk.unspot.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Volume_up

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
	val items = viewModel.libraryItems

	LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(items) { item ->
			LibraryItemRow(item, onClick = { })
		}
	}
}

@Composable
fun LibraryItemRow(item: LibraryItem, onClick: () -> Unit) {
	val textColor =
		if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
	val fontWeight = if (item.isActive) FontWeight.Bold else FontWeight.Normal

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
				.size(56.dp)
				.clip(RoundedCornerShape(4.dp)),
			contentScale = ContentScale.Crop
		)

		Spacer(modifier = Modifier.width(16.dp))

		Column(modifier = Modifier.weight(1f)) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = item.title,
					style = MaterialTheme.typography.bodyLarge,
					color = textColor,
					fontWeight = fontWeight,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f, fill = false)
				)
				if (item.isActive) {
					Spacer(modifier = Modifier.width(4.dp))
					Icon(
						imageVector = MaterialSymbols.OutlinedFilled.Volume_up,
						contentDescription = "Playing",
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(16.dp)
					)
				}
			}
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

@ThemePreview
@Composable
fun LibraryScreenPreview() {
	PreviewScreen {
		// Mock data for preview
		val mockItems = listOf(
			LibraryItem(
				0, "playlist", "https://example.com/c1.jpg", "Liked Songs", "Playlist • 123 songs", true
			), LibraryItem(1, "artist", "https://example.com/c2.jpg", "Artist Name", "Artist", false)
		)
		LazyColumn {
			items(mockItems) { item ->
				LibraryItemRow(item, onClick = {})
			}
		}
	}
}
