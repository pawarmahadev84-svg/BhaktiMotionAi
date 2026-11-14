package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SaffronEmber
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class DepthParticle(
    val xFrac: Float,
    val yOffset: Float,
    val radiusDp: Float,
    val speed: Float,
    val depthLayer: Int,
    val phaseShift: Float
)

@Composable
fun Cinematic3DSceneStage(
    sceneTitle: String,
    sceneNumber: Int,
    totalScenes: Int,
    sceneDescription: String,
    cameraDirection: String,
    lightingMood: String,
    renderStyle3D: String,
    subtitleText: String,
    aspectRatio: String,
    isPlaying: Boolean,
    isEmpty: Boolean = false,
    referenceImagePath: String = "",
    characterImagePath: String = "",
    characterLabel: String = "",
    anamorphicBars: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    var manualYaw by remember { mutableFloatStateOf(0f) }
    var manualPitch by remember { mutableFloatStateOf(0f) }

    val smoothManualYaw by animateFloatAsState(
        targetValue = manualYaw,
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.72f),
        label = "manual_yaw"
    )
    val smoothManualPitch by animateFloatAsState(
        targetValue = manualPitch,
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.72f),
        label = "manual_pitch"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "cinema_3d_loop")
    val cameraCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 7000 else 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera_cycle"
    )
    val particleClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_clock"
    )

    // Parse camera trajectory from cameraDirection / selected 3D rig
    val lowerCam = cameraDirection.lowercase()
    val motionAmp = if (isPlaying) 1.0f else 0.42f
    val autoYaw: Float
    val autoPitch: Float
    val autoScale: Float
    val autoShiftX: Float
    val autoShiftY: Float

    when {
        "orbit" in lowerCam || "arc" in lowerCam || "360" in lowerCam -> {
            autoYaw = ((cameraCycle - 0.5f) * 14f) * motionAmp
            autoPitch = (sin(cameraCycle * PI).toFloat() * 3.5f) * motionAmp
            autoScale = 1.16f + (sin(cameraCycle * PI).toFloat() * 0.05f * motionAmp)
            autoShiftX = ((cameraCycle - 0.5f) * -32f) * motionAmp
            autoShiftY = 0f
        }
        "crane" in lowerCam || "aerial" in lowerCam || "drone" in lowerCam -> {
            autoYaw = ((cameraCycle - 0.5f) * 6f) * motionAmp
            autoPitch = (7f - cameraCycle * 11f) * motionAmp
            autoScale = 1.22f - (cameraCycle * 0.08f * motionAmp)
            autoShiftX = ((cameraCycle - 0.5f) * 14f) * motionAmp
            autoShiftY = ((0.5f - cameraCycle) * 26f) * motionAmp
        }
        "low" in lowerCam || "hero" in lowerCam || "steadicam" in lowerCam -> {
            autoYaw = (sin(cameraCycle * 2f * PI).toFloat() * 4.5f) * motionAmp
            autoPitch = (5.5f + cos(cameraCycle * PI).toFloat() * 2.5f) * motionAmp
            autoScale = 1.15f + (cameraCycle * 0.08f * motionAmp)
            autoShiftX = (cos(cameraCycle * 2f * PI).toFloat() * 14f) * motionAmp
            autoShiftY = (sin(cameraCycle * PI).toFloat() * -10f) * motionAmp
        }
        else -> {
            // Anamorphic Dolly Push-in + subtle 3D parallax
            autoYaw = ((cameraCycle - 0.5f) * 8f) * motionAmp
            autoPitch = ((0.5f - cameraCycle) * 4f) * motionAmp
            autoScale = 1.12f + (cameraCycle * 0.14f * motionAmp)
            autoShiftX = ((cameraCycle - 0.5f) * -20f) * motionAmp
            autoShiftY = ((0.5f - cameraCycle) * 10f) * motionAmp
        }
    }

    val totalYaw = (autoYaw + smoothManualYaw).coerceIn(-18f, 18f)
    val totalPitch = (autoPitch + smoothManualPitch).coerceIn(-14f, 14f)

    // Choose the 3D environment artwork matching the scene keywords
    val backdropDrawable = remember(sceneTitle, sceneDescription, lightingMood, sceneNumber, isEmpty) {
        if (isEmpty) {
            R.drawable.img_mythic_stage_empty
        } else {
            val haystack = "$sceneTitle $sceneDescription $lightingMood".lowercase()
            when {
                "ocean" in haystack || "samudra" in haystack || "sea" in haystack ||
                    "wave" in haystack || "cosmic" in haystack || "nebula" in haystack ||
                    "lanka" in haystack -> R.drawable.img_3d_cosmic_ocean
                "himalay" in haystack || "kailash" in haystack || "ganga" in haystack ||
                    "mountain" in haystack || "temple" in haystack || "snow" in haystack ||
                    "waterfall" in haystack || "monsoon" in haystack -> R.drawable.img_3d_himalayan_temple
                else -> when (sceneNumber % 3) {
                    1 -> R.drawable.img_studio_hero
                    2 -> R.drawable.img_3d_cosmic_ocean
                    else -> R.drawable.img_3d_himalayan_temple
                }
            }
        }
    }

    val particles = remember {
        List(24) { idx ->
            DepthParticle(
                xFrac = ((idx * 37 + 13) % 100) / 100f,
                yOffset = ((idx * 53 + 7) % 100) / 100f,
                radiusDp = when (idx % 3) {
                    0 -> 1.6f
                    1 -> 2.8f
                    else -> 4.2f
                },
                speed = 0.65f + (idx % 4) * 0.25f,
                depthLayer = idx % 3,
                phaseShift = (idx * 0.41f)
            )
        }
    }

    val (rayColorPrimary, rayColorSecondary) = remember(lightingMood) {
        val lower = lightingMood.lowercase()
        when {
            "cosmic" in lower || "nebula" in lower -> Color(0xFFFFB74D) to Color(0xFF7E57C2)
            "mist" in lower || "monsoon" in lower || "himalay" in lower -> Color(0xFFFFE082) to Color(0xFF4DD0E1)
            "aarti" in lower || "golden" in lower -> SaffronEmber to RadiantGold
            else -> RadiantGold to SacredGold
        }
    }

    val hasCustomRef = referenceImagePath.isNotBlank() && File(referenceImagePath).exists()
    val hasForegroundChar = characterImagePath.isNotBlank() &&
        File(characterImagePath).exists() &&
        characterImagePath != referenceImagePath

    val outerContainerModifier = modifier
        .fillMaxWidth()
        .height(if (aspectRatio == "9:16") 290.dp else 232.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(ObsidianBackground)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    manualYaw = 0f
                    manualPitch = 0f
                },
                onDragCancel = {
                    manualYaw = 0f
                    manualPitch = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    manualYaw = (manualYaw + dragAmount.x * 0.14f).coerceIn(-14f, 14f)
                    manualPitch = (manualPitch - dragAmount.y * 0.11f).coerceIn(-10f, 10f)
                }
            )
        }

    Box(modifier = outerContainerModifier, contentAlignment = Alignment.Center) {
        val stageFrameModifier = if (aspectRatio == "9:16") {
            Modifier
                .fillMaxHeight()
                .aspectRatio(9f / 16f)
                .widthIn(min = 156.dp)
                .clip(RoundedCornerShape(10.dp))
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = stageFrameModifier) {
            // LAYER 1: Deep 3D Environment Backdrop
            val backdropModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = 14f * density
                    rotationY = totalYaw
                    rotationX = totalPitch
                    scaleX = autoScale
                    scaleY = autoScale
                    translationX = autoShiftX
                    translationY = autoShiftY
                }

            if (hasCustomRef) {
                AsyncImage(
                    model = File(referenceImagePath),
                    contentDescription = "3D scene source frame",
                    contentScale = ContentScale.Crop,
                    modifier = backdropModifier
                )
            } else {
                Image(
                    painter = painterResource(id = backdropDrawable),
                    contentDescription = "3D mythological cinema backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = backdropModifier
                )
            }

            // LAYER 2: Volumetric 3D God Rays + Depth Embers
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 12f * density
                        rotationY = totalYaw * 1.15f
                        rotationX = totalPitch * 1.15f
                    }
            ) {
                val w = size.width
                val h = size.height

                // Volumetric diagonal sun/divine light shafts
                val sweep = (cameraCycle - 0.5f) * w * 0.18f
                val rayAlpha = if (isPlaying) 0.22f else 0.14f
                for (i in 0..2) {
                    val startX = w * (0.18f + i * 0.28f) + sweep * (1f + i * 0.2f)
                    val path = Path().apply {
                        moveTo(startX - w * 0.06f, 0f)
                        lineTo(startX + w * 0.07f, 0f)
                        lineTo(startX + w * 0.22f, h)
                        lineTo(startX - w * 0.04f, h)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                rayColorPrimary.copy(alpha = rayAlpha),
                                rayColorSecondary.copy(alpha = rayAlpha * 0.45f),
                                Color.Transparent
                            )
                        )
                    )
                }

                // Warm rim-light halo at optical center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            rayColorPrimary.copy(alpha = if (isPlaying) 0.22f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f + totalYaw * 4f, h * 0.36f - totalPitch * 3f),
                        radius = w * 0.55f
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.5f + totalYaw * 4f, h * 0.36f - totalPitch * 3f)
                )

                // Depth-sorted drifting 3D embers / sacred dust motes
                particles.forEach { p ->
                    val progress = (particleClock * p.speed + p.yOffset) % 1f
                    val parallaxFactor = when (p.depthLayer) {
                        0 -> 0.5f
                        1 -> 1.15f
                        else -> 1.9f
                    }
                    val px = ((p.xFrac * w) + sin((progress * 2 * PI) + p.phaseShift).toFloat() * 14f - totalYaw * 2.2f * parallaxFactor)
                        .let { x -> ((x % w) + w) % w }
                    val py = h * (1f - progress) + totalPitch * 1.6f * parallaxFactor
                    val r = p.radiusDp * density * (if (p.depthLayer == 2) 1.25f else 1f)
                    val alpha = (sin(progress * PI).toFloat() * (0.45f + p.depthLayer * 0.2f)).coerceIn(0f, 0.9f)

                    drawCircle(
                        color = rayColorPrimary.copy(alpha = alpha * 0.35f),
                        radius = r * 2.2f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFFFFF3CC).copy(alpha = alpha),
                        radius = r,
                        center = Offset(px, py)
                    )
                }
            }

            // LAYER 3: Cinematic Gradient Grading & Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBackground.copy(alpha = 0.62f),
                                Color.Transparent,
                                Color.Transparent,
                                ObsidianBackground.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // LAYER 4: Parallax Foreground Character Reference Card (when separate character image is attached)
            if (hasForegroundChar) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, SacredGold.copy(alpha = 0.85f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                        .size(width = 76.dp, height = 98.dp)
                        .graphicsLayer {
                            cameraDistance = 10f * density
                            rotationY = (totalYaw * 1.45f) - 10f
                            rotationX = totalPitch * 1.2f
                            translationX = autoShiftX * -0.55f
                            translationY = autoShiftY * -0.45f
                        }
                ) {
                    Box {
                        AsyncImage(
                            model = File(characterImagePath),
                            contentDescription = "3D foreground character reference",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (characterLabel.isNotBlank()) {
                            Text(
                                text = characterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = RadiantGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(ObsidianBackground.copy(alpha = 0.82f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // LAYER 5: Anamorphic Letterbox Cinema Bars (for 16:9 widescreen cinema feel)
            if (anamorphicBars && aspectRatio == "16:9") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color.Black.copy(alpha = 0.78f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color.Black.copy(alpha = 0.78f))
                )
            }

            // LAYER 6: 3D Cinema Telemetry HUD
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (anamorphicBars && aspectRatio == "16:9") 18.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianBackground.copy(alpha = 0.78f),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, ObsidianOutlineGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraEnhance,
                            contentDescription = null,
                            tint = RadiantGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEmpty) {
                                "3D STAGE READY"
                            } else {
                                "SCENE $sceneNumber/${totalScenes.coerceAtLeast(1)} • 3D"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantGold,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    color = ObsidianBackground.copy(alpha = 0.76f),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ViewInAr,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPlaying) "LIVE 3D" else "DRAG TO ORBIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // LAYER 7: Center Empty Hint OR Bottom Subtitle + Rig Telemetry
            if (isEmpty) {
                Text(
                    text = "Plan scenes in Director to preview 3D camera parallax, volumetric lighting, and narration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = if (anamorphicBars && aspectRatio == "16:9") 18.dp else 10.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFF8E1),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$renderStyle3D • ${cameraDirection.ifBlank { "35mm Anamorphic Dolly" }} • $lightingMood",
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantGold.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
