package com.expensetracker.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.expensetracker.app.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkPrimaryBright,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
)

/** Extra colors shared across screens that aren't part of the M3 scheme. */
@Immutable
data class AppColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val glow: Color,
    val cardStroke: Color,
    /** Background of the hero "spent" cards. */
    val heroBrush: Brush,
    /** App-wide background (solid-ish in dark, mint gradient in light). */
    val backgroundBrush: Brush,
    /** Card fill (solid in dark, white -> very light grey in light). */
    val cardBrush: Brush,
)

private val DarkAppColors = AppColors(
    gradientStart = DarkPrimary,
    gradientEnd = Color(0xFF5089FF),
    glow = DarkPrimary.copy(alpha = 0.35f),
    cardStroke = Color(0xFF23293D),
    heroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3A76FF),
            Color(0xFF5089FF),
            Color(0xFF3A76FF),
        )
    ),
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0E1220),
            DarkBackground,
            Color(0xFF080A10),
        )
    ),
    cardBrush = Brush.verticalGradient(
        colors = listOf(
            DarkSurface,
            Color(0xFF101422),
        )
    ),
)

private val LightAppColors = AppColors(
    gradientStart = LightPrimary,
    gradientEnd = Color(0xFF1D4ED8),
    glow = LightPrimary.copy(alpha = 0.25f),
    cardStroke = Color(0xFFE0E9E5),
    heroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1A1A),
            Color(0xFF525252),
        )
    ),
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            LightMintTop,
            LightBackground,
            LightMintEdge,
        )
    ),
    cardBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF2F6F4),
        )
    ),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

@Composable
fun ExpenseTrackerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (dark) DarkColors else LightColors
    val appColors = if (dark) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
