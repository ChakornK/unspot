package com.chakornk.unspot.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Close
import com.composables.icons.materialsymbols.outlined.Search
import com.composables.icons.materialsymbols.outlinedfilled.Volume_up

@Composable
fun LibraryScreen(
	viewModel: LibraryViewModel, onSetTopBar: (@Composable () -> Unit) -> Unit
) {
	val items = viewModel.filteredItems
	var isSearchActive by remember { mutableStateOf(false) }

	LaunchedEffect(isSearchActive, viewModel.searchQuery) {
		onSetTopBar {
			LibraryTopBar(
				searchQuery = viewModel.searchQuery,
				onSearchQueryChange = { viewModel.searchQuery = it },
				isSearchActive = isSearchActive,
				onSearchToggle = {
					isSearchActive = !isSearchActive
					if (!isSearchActive) viewModel.searchQuery = ""
				})
		}
	}

	DisposableEffect(Unit) {
		onDispose {
			onSetTopBar {}
		}
	}

	LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(items, key = { it.uri }) { item ->
			LibraryItemRow(item, onClick = { })
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
	searchQuery: String,
	onSearchQueryChange: (String) -> Unit,
	isSearchActive: Boolean,
	onSearchToggle: () -> Unit
) {
	val focusRequester = remember { FocusRequester() }
	val focusManager = LocalFocusManager.current

	TopAppBar(title = {
		if (isSearchActive) {
			BasicTextField(
				value = searchQuery,
				onValueChange = onSearchQueryChange,
				modifier = Modifier
					.fillMaxWidth()
					.focusRequester(focusRequester),
				textStyle = MaterialTheme.typography.bodyLarge.merge(color = MaterialTheme.colorScheme.onSurface),
				singleLine = true,
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
				keyboardActions = KeyboardActions(onSearch = {
					focusManager.clearFocus()
				}),
				decorationBox = { innerTextField ->
					if (searchQuery.isEmpty()) {
						Text(
							text = "Search library",
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
						)
					}
					innerTextField()
				},
				cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
			)
			LaunchedEffect(Unit) {
				focusRequester.requestFocus()
			}
		} else {
			Text("Your Library")
		}
	}, actions = {
		IconButton(onClick = onSearchToggle) {
			Icon(
				imageVector = if (isSearchActive) MaterialSymbols.Outlined.Close else MaterialSymbols.Outlined.Search,
				contentDescription = if (isSearchActive) "Close search" else "Search"
			)
		}
	})
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
				.clip(RoundedCornerShape(4.dp))
				.background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)),
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
	PreviewScreen(
		topBar = {
			LibraryTopBar(
				searchQuery = "",
				onSearchQueryChange = {},
				isSearchActive = true,
				onSearchToggle = {})
		}) {
		// Mock data for preview
		val mockItems = listOf(
			LibraryItem(
				"1", "playlist", "https://example.com/c1.jpg", "Liked Songs", "Playlist • John Doe", true
			), LibraryItem("2", "artist", "https://example.com/c2.jpg", "Artist Name", "Artist", false)
		)
		LazyColumn {
			items(mockItems) { item ->
				LibraryItemRow(item, onClick = {})
			}
		}
	}
}
