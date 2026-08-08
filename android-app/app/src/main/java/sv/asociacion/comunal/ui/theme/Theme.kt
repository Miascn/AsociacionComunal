package sv.asociacion.comunal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1769E0)
private val LightColors = lightColorScheme(primary = Blue, secondary = Color(0xFF345F91), surface = Color(0xFFF8FAFF))
private val DarkColors = darkColorScheme(primary = Color(0xFF8BB8FF), secondary = Color(0xFFAAC7F0), surface = Color(0xFF0D1B2E))

@Composable fun AsociacionTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
