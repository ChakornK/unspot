package com.chakornk.unspot.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(private val model: WelcomeModel = WelcomeModel()) : ViewModel() {
	sealed class WelcomeEvent {
		data class NavigateToLogin(val route: String) : WelcomeEvent()
		data class OpenSignUp(val url: String) : WelcomeEvent()
	}

	private val _events = MutableSharedFlow<WelcomeEvent>()
	val events = _events.asSharedFlow()

	fun onSignInClick() {
		viewModelScope.launch {
			_events.emit(WelcomeEvent.NavigateToLogin(model.getLoginRoute()))
		}
	}

	fun onSignUpClick() {
		viewModelScope.launch {
			_events.emit(WelcomeEvent.OpenSignUp(model.signUpUrl))
		}
	}
}


