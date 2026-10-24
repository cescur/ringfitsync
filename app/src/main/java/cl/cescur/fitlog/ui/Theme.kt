package cl.cescur.fitlog.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFE8580C)
val AccentSoft = Color(0xFFFDE9DE)
val Ok = Color(0xFF2E7D32)
val Warn = Color(0xFFB26A00)
val Muted = Color(0xFF5F6B7A)

private val colors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Color(0xFF5A1E00),
    secondary = Accent,
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFF6F7F9),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    onSurfaceVariant = Muted,
    error = Color(0xFFC62828),
)

@Composable
fun FitLogTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
