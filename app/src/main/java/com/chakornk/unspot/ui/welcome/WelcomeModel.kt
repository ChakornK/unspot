package com.chakornk.unspot.ui.welcome

import com.chakornk.unspot.ui.navigation.View

class WelcomeModel {
	val signUpUrl: String = "https://www.spotify.com/signup"

	fun getLoginRoute(): String = View.Login.route
}
