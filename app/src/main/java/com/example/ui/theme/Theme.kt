package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = FrostCyanLight,
  onPrimary = Slate950,
  primaryContainer = Slate800,
  onPrimaryContainer = FrostCyanLight,
  secondary = FlameOrangeLight,
  onSecondary = Slate950,
  secondaryContainer = Slate800,
  onSecondaryContainer = FlameOrangeLight,
  tertiary = GaugeGreenLight,
  onTertiary = Slate950,
  background = Slate950,
  onBackground = Color.White,
  surface = Slate900,
  onSurface = Color.White,
  surfaceVariant = Slate800,
  onSurfaceVariant = Slate300,
  outline = Slate600,
  error = AlertRedLight,
  onError = Slate950
)

private val LightColorScheme = lightColorScheme(
  primary = HvacBlueDark,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = HvacBlueDark,
  secondary = FlameOrange,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFFEDD5),
  onSecondaryContainer = FlameOrange,
  tertiary = GaugeGreen,
  onTertiary = Color.White,
  background = Slate50,
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate100,
  onSurfaceVariant = Slate700,
  outline = Slate300,
  error = AlertRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false, // Use our crisp high-contrast HVAC colors by default
  content: @Composable () -> Unit,
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
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
