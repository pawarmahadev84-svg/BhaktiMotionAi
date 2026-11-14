package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BhaktiDarkColorScheme = darkColorScheme(
    primary = SacredGold,
    onPrimary = OnGoldDark,
    primaryContainer = DeepBronze,
    onPrimaryContainer = RadiantGold,
    secondary = SaffronEmber,
    onSecondary = OnGoldDark,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = RadiantGold,
    tertiary = LotusRose,
    onTertiary = OnGoldDark,
    background = ObsidianBackground,
    onBackground = TempleIvory,
    surface = ObsidianSurface,
    onSurface = TempleIvory,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = MutedBronze,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineGold,
    error = CrimsonError,
    onError = OnGoldDark,
    errorContainer = CrimsonContainer,
    onErrorContainer = TempleIvory
)

val BhaktiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BhaktiMotionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BhaktiDarkColorScheme,
        typography = BhaktiTypography,
        shapes = BhaktiShapes,
        content = content
    )
}
