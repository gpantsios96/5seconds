package app.anasa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Fallback palette (used before Android 12, where there are no dynamic colours): calm moss and sand.
private val Light = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4EFD6),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF7A5900),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDEA6),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = Color(0xFF8B4A62),
    tertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFFF6F1EA),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFF6F1EA),
    onSurface = Color(0xFF1C1B18),
    surfaceContainer = Color(0xFFECE6DD),
    surfaceContainerHigh = Color(0xFFE5DFD6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF98D3BB),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF145040),
    onPrimaryContainer = Color(0xFFB4EFD6),
    secondary = Color(0xFFF1C048),
    secondaryContainer = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFDEA6),
    tertiary = Color(0xFFFFB0C9),
    tertiaryContainer = Color(0xFF6F334A),
    background = Color(0xFF131411),
    onBackground = Color(0xFFE6E2DA),
    surface = Color(0xFF131411),
    onSurface = Color(0xFFE6E2DA),
    surfaceContainer = Color(0xFF1F201C),
    surfaceContainerHigh = Color(0xFF2A2A26),
)

// Expressive: generous, rounded shapes.
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

private val base = Typography()
private val ExpressiveType = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnasaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = ExpressiveType,
        content = content,
    )
}
