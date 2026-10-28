package app.todo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StarColor = Color(0xFFF5A524)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E8FC),
    onPrimaryContainer = Color(0xFF1C2E8A),
    secondaryContainer = Color(0xFFE3E8FC),
    onSecondaryContainer = Color(0xFF1C2E8A),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF18181B),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFEFEFEA),
    onSurfaceVariant = Color(0xFF6B6B70),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F1),
    surfaceContainer = Color(0xFFF1F1EC),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEFEFEA),
    outline = Color(0xFF9A9AA0),
    outlineVariant = Color(0xFFE4E4DE),
    error = Color(0xFFD92D3A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA6FF),
    onPrimary = Color(0xFF0B1B66),
    primaryContainer = Color(0xFF26325F),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondaryContainer = Color(0xFF26325F),
    onSecondaryContainer = Color(0xFFDDE3FF),
    background = Color(0xFF0F0F11),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF0F0F11),
    onSurface = Color(0xFFEDEDEF),
    surfaceVariant = Color(0xFF212125),
    onSurfaceVariant = Color(0xFF9B9BA3),
    surfaceContainerLowest = Color(0xFF0A0A0B),
    surfaceContainerLow = Color(0xFF151517),
    surfaceContainer = Color(0xFF1A1A1D),
    surfaceContainerHigh = Color(0xFF212125),
    surfaceContainerHighest = Color(0xFF2A2A2F),
    outline = Color(0xFF6E6E76),
    outlineVariant = Color(0xFF2A2A2F),
    error = Color(0xFFFF6B74)
)

@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
