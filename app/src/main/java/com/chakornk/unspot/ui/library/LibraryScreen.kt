package com.chakornk.unspot.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview

@Composable
fun LibraryScreen() {
	Box(
		modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
	) {
		Text(text = "Library Screen")
	}
}

@ThemePreview
@Composable
fun LibraryScreenPreview() {
	PreviewScreen {
		LibraryScreen()
	}
}
