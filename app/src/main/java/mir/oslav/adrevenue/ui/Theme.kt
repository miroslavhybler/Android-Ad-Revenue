package mir.oslav.adrevenue.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    // Primary — Insight Violet
    primary = Color(0xFFC9C1FF),
    onPrimary = Color(0xFF302568),
    primaryContainer = Color(0xFF494080),
    onPrimaryContainer = Color(0xFFE5DEFF),
    inversePrimary = Color(0xFF5D54C5),

    // Secondary — Data Cyan
    secondary = Color(0xFF79D4E0),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9AF0FC),

    // Tertiary — Revenue Amber
    tertiary = Color(0xFFF8C074),
    onTertiary = Color(0xFF422B00),
    tertiaryContainer = Color(0xFF5E4100),
    onTertiaryContainer = Color(0xFFFFDEA8),

    // Main dark surfaces
    background = Color(0xFF121217),
    onBackground = Color(0xFFE7E1EA),
    surface = Color(0xFF121217),
    onSurface = Color(0xFFE7E1EA),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC3CF),
    surfaceTint = Color(0xFFC9C1FF),

    // Tone-based surface hierarchy
    surfaceDim = Color(0xFF121217),
    surfaceBright = Color(0xFF39373F),
    surfaceContainerLowest = Color(0xFF0D0D12),
    surfaceContainerLow = Color(0xFF1A191F),
    surfaceContainer = Color(0xFF1E1D23),
    surfaceContainerHigh = Color(0xFF28262D),
    surfaceContainerHighest = Color(0xFF333139),

    // Outlines
    outline = Color(0xFF948E99),
    outlineVariant = Color(0xFF49454F),

    // Inverse surfaces
    inverseSurface = Color(0xFFE7E1EA),
    inverseOnSurface = Color(0xFF302E34),

    // Error
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Fixed accent roles
    primaryFixed = Color(0xFFE5DEFF),
    primaryFixedDim = Color(0xFFC9C1FF),
    onPrimaryFixed = Color(0xFF1A0F5E),
    onPrimaryFixedVariant = Color(0xFF494080),

    secondaryFixed = Color(0xFF9AF0FC),
    secondaryFixedDim = Color(0xFF79D4E0),
    onSecondaryFixed = Color(0xFF001F24),
    onSecondaryFixedVariant = Color(0xFF004F58),

    tertiaryFixed = Color(0xFFFFDEA8),
    tertiaryFixedDim = Color(0xFFF8C074),
    onTertiaryFixed = Color(0xFF281800),
    onTertiaryFixedVariant = Color(0xFF5E4100),

    scrim = Color(0xFF000000),
)

val LightColorScheme = lightColorScheme(
    // Primary — Insight Violet
    primary = Color(0xFF5D54C5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1A0F5E),
    inversePrimary = Color(0xFFC9C1FF),

    // Secondary — Data Cyan
    secondary = Color(0xFF006875),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9AF0FC),
    onSecondaryContainer = Color(0xFF001F24),

    // Tertiary — Revenue Amber
    tertiary = Color(0xFF7B5800),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA8),
    onTertiaryContainer = Color(0xFF281800),

    // Main light surfaces
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = Color(0xFF5D54C5),

    // Tone-based surface hierarchy
    surfaceDim = Color(0xFFDDD8E0),
    surfaceBright = Color(0xFFFCF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEAE6EE),
    surfaceContainerHighest = Color(0xFFE4E0E8),

    // Outlines
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCBC4CF),

    // Inverse surfaces
    inverseSurface = Color(0xFF313036),
    inverseOnSurface = Color(0xFFF4EFF7),

    // Error
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Fixed accent roles
    primaryFixed = Color(0xFFE5DEFF),
    primaryFixedDim = Color(0xFFC9C1FF),
    onPrimaryFixed = Color(0xFF1A0F5E),
    onPrimaryFixedVariant = Color(0xFF494080),

    secondaryFixed = Color(0xFF9AF0FC),
    secondaryFixedDim = Color(0xFF79D4E0),
    onSecondaryFixed = Color(0xFF001F24),
    onSecondaryFixedVariant = Color(0xFF004F58),

    tertiaryFixed = Color(0xFFFFDEA8),
    tertiaryFixedDim = Color(0xFFF8C074),
    onTertiaryFixed = Color(0xFF281800),
    onTertiaryFixedVariant = Color(0xFF5E4100),

    scrim = Color(0xFF000000),
)


object ChartMetricColors {
    val DarkRevenue = Color(0xFFC9C1FF)
    val DarkImpressions = Color(0xFF79D4E0)
    val DarkEcpm = Color(0xFFF8C074)
    val DarkRequests = Color(0xFF82B1FF)
    val DarkMatchRate = Color(0xFF78D8A4)
    val DarkClicks = Color(0xFFF2A7C6)

    val LightRevenue = Color(0xFF5D54C5)
    val LightImpressions = Color(0xFF006875)
    val LightEcpm = Color(0xFF7B5800)
    val LightRequests = Color(0xFF0061A4)
    val LightMatchRate = Color(0xFF006C48)
    val LightClicks = Color(0xFF93436B)
}

@Composable
fun AdRevenueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
fun PreviewContainer(
    content: @Composable () -> Unit,
) {
    AdRevenueTheme(
        content = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    )

}