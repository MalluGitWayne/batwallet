package com.example.walletlookup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Batwallet's look, the same as Batbloom's: neutral greys like iOS's grouped lists (no tint), cards one step lighter
 * than the background (white in Light), no borders, and each tab in its own colour.
 */
data class Palette(
    val background: Color,
    val panel: Color,
    val muted: Color,
    val isLight: Boolean,
    val black: Boolean = false,
)

val DarkPalette = Palette(Color(0xFF111113), Color(0xFF1E1E21), Color(0xFF98989F), isLight = false)
val OledPalette = Palette(Color(0xFF000000), Color(0xFF161618), Color(0xFF8E8E93), isLight = false, black = true)
val LightPalette = Palette(Color(0xFFF2F2F7), Color(0xFFFFFFFF), Color(0xFF6E6E73), isLight = true)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Grey text for secondary lines, matching the theme. */
val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted

/**
 * Each tab has its own colour, the way Apple's apps do: buttons, switches, chips and highlights on that tab take it.
 * Dark themes get the brighter shade, Light the deeper one so it reads on white. Home keeps Batwallet's gold.
 */
enum class TabAccent(val dark: Color, val light: Color) {
    Home(Color(0xFFFFC83D), Color(0xFF9A6700)),     // gold
    Wallets(Color(0xFF0A84FF), Color(0xFF0066D6)),  // blue
    Settings(Color(0xFF8E8E93), Color(0xFF6E6E73)), // grey
    ;

    @Composable @ReadOnlyComposable
    fun color(): Color = if (LocalPalette.current.isLight) light else dark
}

/** Green for money in, red for money out, in the shade that reads on this theme. */
val UpGreen: Color @Composable @ReadOnlyComposable get() = if (LocalPalette.current.isLight) Color(0xFF1F8A3A) else Color(0xFF30D158)
val DownRed: Color @Composable @ReadOnlyComposable get() = if (LocalPalette.current.isLight) Color(0xFFD70015) else Color(0xFFFF453A)

private fun onAccent(c: Color) = if (c.luminance() > 0.45f) Color(0xFF0B0B0F) else Color.White

/** Runs [content] in a tab's colour: primary buttons, switches, radio buttons and selection all follow it. */
@Composable
fun TabTheme(accent: TabAccent, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    // Settings stays grey, but its switches and buttons keep iOS's blue so they still look tappable.
    val action = (if (accent == TabAccent.Settings) TabAccent.Wallets else accent).let { if (p.isLight) it.light else it.dark }
    val scheme = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = scheme.copy(
            primary = action, onPrimary = onAccent(action),
            primaryContainer = action.copy(alpha = if (p.isLight) 0.14f else 0.22f).compositeOver(p.panel),
            onPrimaryContainer = action,
        ),
        typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content,
    )
}

private fun schemeFor(p: Palette): ColorScheme {
    val gold = if (p.isLight) TabAccent.Home.light else TabAccent.Home.dark
    return if (p.isLight) lightColorScheme(
        primary = gold, onPrimary = onAccent(gold),
        primaryContainer = gold.copy(alpha = 0.14f).compositeOver(p.panel), onPrimaryContainer = gold,
        secondary = Color(0xFF1C1C1E), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5E5EA), onSecondaryContainer = Color(0xFF1C1C1E),
        tertiary = Color(0xFF00896F),
        background = p.background, onBackground = Color(0xFF1C1C1E),
        surface = p.background, onSurface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFFE5E5EA), onSurfaceVariant = p.muted,
        // Cards (Material's default card colour is surfaceContainerHighest) and Settings groups are the white panels.
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9F9FB),
        surfaceContainer = p.panel, surfaceContainerHigh = p.panel, surfaceContainerHighest = p.panel,
        outline = Color(0xFFC7C7CC), outlineVariant = Color(0xFFE5E5EA),
        error = Color(0xFFD70015), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    ) else darkColorScheme(
        primary = gold, onPrimary = onAccent(gold),
        primaryContainer = gold.copy(alpha = 0.22f).compositeOver(p.panel), onPrimaryContainer = gold,
        secondary = Color(0xFFF2F2F7), onSecondary = Color(0xFF111113),
        secondaryContainer = if (p.black) Color(0xFF1C1C1E) else Color(0xFF2C2C2E), onSecondaryContainer = Color(0xFFF2F2F7),
        tertiary = Color(0xFF63E6BE),
        background = p.background, onBackground = Color(0xFFF2F2F7),
        surface = p.background, onSurface = Color(0xFFF2F2F7),
        surfaceVariant = if (p.black) Color(0xFF1C1C1E) else Color(0xFF2C2C2E), onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.background, surfaceContainerLow = if (p.black) Color(0xFF0A0A0B) else Color(0xFF161618),
        surfaceContainer = p.panel, surfaceContainerHigh = p.panel, surfaceContainerHighest = p.panel,
        outline = Color(0xFF48484A), outlineVariant = Color(0xFF2C2C2E),
        error = Color(0xFFFF6961), errorContainer = Color(0xFF5C1A1A), onErrorContainer = Color(0xFFFFDAD6),
    )
}

/** System follows the phone's dark mode (Dark or Light). */
@Composable
fun paletteFor(mode: ThemeMode): Palette = when (mode) {
    ThemeMode.DIM -> DarkPalette
    ThemeMode.DARK -> OledPalette
    ThemeMode.LIGHT -> LightPalette
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkPalette else LightPalette
}

/** Large titles like iOS: bold, tight. Cards: 20dp corners. */
fun batTypography(base: Typography): Typography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
)

private val BatShapes = Shapes(medium = RoundedCornerShape(20.dp))

@Composable
fun BatTheme(palette: Palette, typography: Typography, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette), typography = batTypography(typography), shapes = BatShapes, content = content)
    }
}

/** A small white symbol on a coloured rounded square, like iPhone Settings rows. Either [icon] or [glyph]. */
@Composable
fun IconTile(color: Color, icon: ImageVector? = null, glyph: String? = null) {
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        else if (glyph != null) Text(glyph, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, translate = false)
    }
}

/** The header of a summary card: a small symbol and the name, both in that area's colour (Health-app style). */
@Composable
fun CardHeader(title: String, icon: ImageVector, accent: TabAccent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent.color(), modifier = Modifier.size(15.dp))
        Text(title, color = accent.color(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}

/** iOS-style segmented control: a grey track with the chosen option on a raised pill. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val track = if (p.isLight) Color(0xFFE3E3E8) else if (p.black) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
    val thumb = if (p.isLight) Color.White else Color(0xFF5A5A5F)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(track).padding(2.dp)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (i == selected) thumb else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
                    fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
