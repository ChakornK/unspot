package com.chakornk.unspot.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview

@Composable
fun HomeScreen() {
	Box(
		modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
	) {
		Text(text = "Home Screen")
	}
}

@ThemePreview
@Composable
fun HomeScreenPreview() {
	PreviewScreen {
		HomeScreen()
	}
}
