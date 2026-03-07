package app.tsosu.ui.theme

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

private val BrandOrange = Color(0xFFFF7043)
private val BrandOrangeDark = Color(0xFFFFAB91)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF77574D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C160E),
    tertiary = Color(0xFF6C5D2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E1A7),
    onTertiaryContainer = Color(0xFF231B00),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF442A21),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFFD9C58D),
    onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534519),
    onTertiaryContainer = Color(0xFFF6E1A7),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
)

@Composable
fun TsosuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
