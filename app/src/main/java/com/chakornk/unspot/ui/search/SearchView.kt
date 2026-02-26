package com.chakornk.unspot.ui.search

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
fun SearchScreen(
	sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope
) {
	sharedTransitionScope.apply {
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("Search") }, modifier = Modifier.sharedBounds(
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
				Text(text = "Search Screen")
			}
		}
	}
}

@ThemePreview
@Composable
fun SearchScreenPreview() {
	PreviewScreen {
		Text("Search Screen Preview")
	}
}
