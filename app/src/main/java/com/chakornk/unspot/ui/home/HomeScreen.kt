package com.chakornk.unspot.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chakornk.unspot.ui.theme.PreviewScreen
import com.chakornk.unspot.ui.theme.ThemePreview

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
	sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope
) {
	sharedTransitionScope.apply {
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("Home") }, modifier = Modifier.sharedBounds(
						rememberSharedContentState(key = "top-bar"),
						animatedVisibilityScope = animatedVisibilityScope
					)
				)
			}) { padding ->
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding), contentAlignment = Alignment.Center
			) {
				Text(text = "Home Screen")
			}
		}
	}
}

@ThemePreview
@Composable
fun HomeScreenPreview() {
	PreviewScreen {
		// Mock scopes for preview are not easily available, so this might need adjustment if previews are used
		Text("Home Screen Preview")
	}
}
