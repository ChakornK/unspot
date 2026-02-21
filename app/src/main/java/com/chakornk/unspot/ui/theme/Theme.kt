package com.chakornk.unspot.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

private val DarkColorScheme = darkColorScheme()

private val LightColorScheme = lightColorScheme()

@Composable
fun UnspotTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = false, content: @Composable () -> Unit
) {
	val colorScheme = when {
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
			val context = LocalContext.current
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		darkTheme -> DarkColorScheme
		else -> LightColorScheme
	}

	MaterialTheme(
		colorScheme = colorScheme, content = content
	)
}

@Preview(
	name = "light",
	showBackground = true,
	uiMode = Configuration.UI_MODE_NIGHT_NO,
	backgroundColor = 0xfef7ff
)
@Preview(
	name = "dark",
	showBackground = true,
	uiMode = Configuration.UI_MODE_NIGHT_YES,
	backgroundColor = 0x141218
)
annotation class ThemePreview

@Composable
fun PreviewScreen(
	topBar: @Composable () -> Unit = {},
	bottomBar: @Composable () -> Unit = {},
	content: @Composable () -> Unit
) {
	UnspotTheme {
		Scaffold(topBar = topBar, bottomBar = bottomBar, modifier = Modifier.fillMaxSize()) { padding ->
			Box(modifier = Modifier.padding(padding)) {
				content()
			}
		}
	}
}
