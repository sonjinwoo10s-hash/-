package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class IslamicExtendedColors(
    val deepGreen: Color,
    val gold: Color,
    val goldSoft: Color,
    val goldText: Color,
    val emeraldSoft: Color,
    val cardAlt: Color,
    val border: Color,
    val textMuted: Color
)

val LocalIslamicColors = staticCompositionLocalOf {
    IslamicExtendedColors(
        deepGreen = DeepGreen,
        gold = SoftGold,
        goldSoft = GoldSoft,
        goldText = GoldDeepText,
        emeraldSoft = EmeraldSoft,
        cardAlt = CardAltLight,
        border = BorderLight,
        textMuted = TextMutedLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = DeepGreen,
    secondary = SoftGold,
    onSecondary = Color(0xFF1A1405),
    secondaryContainer = GoldSoft,
    onSecondaryContainer = GoldDeepText,
    tertiary = DeepGreen,
    onTertiary = Color.White,
    background = WarmCream,
    onBackground = TextMainLight,
    surface = CardLight,
    onSurface = TextMainLight,
    surfaceVariant = CardAltLight,
    onSurfaceVariant = TextMutedLight,
    outline = BorderLight,
    outlineVariant = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoftDark,
    onPrimaryContainer = Color(0xFF9BE8CE),
    secondary = GoldDark,
    onSecondary = Color(0xFF1A1405),
    secondaryContainer = GoldSoftDark,
    onSecondaryContainer = GoldDark,
    tertiary = DeepGreenDark,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextMainDark,
    surface = CardDark,
    onSurface = TextMainDark,
    surfaceVariant = CardAltDark,
    onSurfaceVariant = TextMutedDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

@Composable
fun RafiqAlMuslimTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        IslamicExtendedColors(
            deepGreen = DeepGreenDark,
            gold = GoldDark,
            goldSoft = GoldSoftDark,
            goldText = GoldDark,
            emeraldSoft = EmeraldSoftDark,
            cardAlt = CardAltDark,
            border = BorderDark,
            textMuted = TextMutedDark
        )
    } else {
        IslamicExtendedColors(
            deepGreen = DeepGreen,
            gold = SoftGold,
            goldSoft = GoldSoft,
            goldText = GoldDeepText,
            emeraldSoft = EmeraldSoft,
            cardAlt = CardAltLight,
            border = BorderLight,
            textMuted = TextMutedLight
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalIslamicColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
