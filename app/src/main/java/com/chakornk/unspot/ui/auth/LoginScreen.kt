package com.chakornk.unspot.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
	var email by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.Top
	) {
		Text(
			text = "Sign In",
			fontSize = 28.sp,
		)
		Spacer(modifier = Modifier.height(32.dp))

		OutlinedTextField(
			value = email,
			onValueChange = { email = it },
			label = { Text("Email or Username") },
			singleLine = true,
			modifier = Modifier
				.fillMaxWidth()
				.semantics { contentType = ContentType.EmailAddress },
			keyboardOptions = KeyboardOptions.Default.copy(
				keyboardType = KeyboardType.Email,
				imeAction = ImeAction.Next,
			),
		)
		Spacer(modifier = Modifier.height(16.dp))
		OutlinedTextField(
			value = password,
			onValueChange = { password = it },
			label = { Text("Password") },
			singleLine = true,
			visualTransformation = PasswordVisualTransformation(),
			modifier = Modifier
				.fillMaxWidth()
				.semantics { contentType = ContentType.Password },
			keyboardOptions = KeyboardOptions.Default.copy(
				keyboardType = KeyboardType.Password,
				imeAction = ImeAction.Done,
			),
		)
		Spacer(modifier = Modifier.weight(1f))

		Button(
			onClick = { viewModel.login(email, password) },
			modifier = Modifier.fillMaxWidth(),
		) {
			Text(text = "Log In", modifier = Modifier.padding(vertical = 8.dp))
		}
	}
}

