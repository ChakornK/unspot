package com.chakornk.unspot.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Home
import com.composables.icons.materialsymbols.outlined.Library_music
import com.composables.icons.materialsymbols.outlined.Search
import com.composables.icons.materialsymbols.outlinedfilled.Home
import com.composables.icons.materialsymbols.outlinedfilled.Library_music
import com.composables.icons.materialsymbols.outlinedfilled.Search

sealed class View(
	val route: String, val label: String
) {
	object Welcome : View(
		"welcome", "Welcome"
	)

	object Login : View("login", "Login")

	object Home : View("home", "Home")

	object Search : View(
		"search", "Search"
	)

	object Library : View("library", "Library")

	object Playlist : View("playlist/{uri}", "Playlist") {
		fun createRoute(uri: String) = "playlist/$uri"
	}
}

sealed class Tab(
	val view: View,
	val route: String,
	val label: String,
	val icon: ImageVector,
	val iconSelected: ImageVector
) {
	object Home : Tab(
		View.Home, "home", "Home", MaterialSymbols.Outlined.Home, MaterialSymbols.OutlinedFilled.Home
	)

	object Search : Tab(
		View.Search,
		"search",
		"Search",
		MaterialSymbols.Outlined.Search,
		MaterialSymbols.OutlinedFilled.Search
	)

	object Library : Tab(
		View.Library,
		"library",
		"Library",
		MaterialSymbols.Outlined.Library_music,
		MaterialSymbols.OutlinedFilled.Library_music
	)
}
