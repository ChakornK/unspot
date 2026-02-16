package com.chakornk.unspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onSignInClick: () -> Unit, onSignUpClick: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top
	) {
		Spacer(modifier = Modifier.weight(1f))

		Text(
			text = "Unspot",
			fontSize = 32.sp,
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(
			text = "A music player.",
			fontSize = 16.sp,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)

		Spacer(modifier = Modifier.weight(1f))

		Button(
			onClick = onSignInClick,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(text = "Sign In", modifier = Modifier.padding(vertical = 8.dp))
		}
		Spacer(modifier = Modifier.height(16.dp))
		FilledTonalButton(
			onClick = onSignUpClick,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(text = "Sign Up", modifier = Modifier.padding(vertical = 8.dp))
		}
	}
}
