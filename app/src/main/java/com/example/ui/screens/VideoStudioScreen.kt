package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.core.MediaStorageHelper
import com.example.core.RenderSaver
import com.example.data.video.GeminiOmniVideoAdapter
import com.example.data.model.Cinematic3DPresets
import com.example.data.model.ModularRenderJob
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.example.data.model.VideoProviderRegistry
import com.example.ui.components.Cinematic3DSceneStage
import com.example.ui.components.GoldAccentDivider
import com.example.ui.components.SacredCard
import com.example.ui.components.ServiceTierBadge
import com.example.ui.theme.AmberPaid
import com.example.ui.theme.AmberPaidContainer
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.DataState
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioScreen(
    scenesState: DataState<List<MovieScene>>,
    charactersState: DataState<List<MythCharacter>>,
    renderJobsState: DataState<List<ModularRenderJob>>,
    videoAiState: AiOperationState,
    onRefinePrompt: (
        draftPrompt: String,
        mode: String,
        aspectRatio: String,
        durationSeconds: Long,
        selectedCharacters: List<MythCharacter>,
        renderStyle3D: String,
        cameraRig3D: String,
        lightingMood3D: String,
        onRefined: (String) -> Unit
    ) -> Unit,
    onQueueRenderJob: (
        provider: com.example.data.model.VideoProviderSpec,
        mode: String,
        prompt: String,
        aspectRatio: String,
        durationSeconds: Long,
        sceneId: String,
        characterNames: List<String>,
        referenceImagePath: String
    ) -> Unit,
    onCancelJob: (String) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scenes = (scenesState as? DataState.Success)?.data ?: emptyList()
    val characters = (charactersState as? DataState.Success)?.data ?: emptyList()
    val jobs = (renderJobsState as? DataState.Success)?.data ?: emptyList()

    var mode by remember { mutableStateOf("text_to_video") }
    var selectedSceneId by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableLongStateOf(8L) }
    var aspectRatio by remember { mutableStateOf("16:9") }
    var renderStyle3D by remember { mutableStateOf(Cinematic3DPresets.renderEngines.first()) }
    var cameraRig3D by remember { mutableStateOf(Cinematic3DPresets.cameraRigs.first()) }
    var lightingMood3D by remember { mutableStateOf(Cinematic3DPresets.lightingMoods.first()) }
    var isPreviewAnimating by remember { mutableStateOf(true) }
    var promptText by remember { mutableStateOf("") }
    var selectedProviderId by remember { mutableStateOf(VideoProviderRegistry.providers.first().id) }
    var selectedCharIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var referenceImagePath by remember { mutableStateOf("") }
    var isRendering by remember { mutableStateOf(false) }

    val selectedProvider = VideoProviderRegistry.providers.first { it.id == selectedProviderId }
    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId }
    val selectedCharacters = characters.filter { it.id in selectedCharIds }
    val primaryCharWithImage = selectedCharacters.firstOrNull { it.localImagePath.isNotBlank() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                referenceImagePath = MediaStorageHelper.copyImageToAppStorage(context, uri, "video_refs")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SacredCard(goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D Cinematic Video Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RadiantGold
                        )
                        ServiceTierBadge(
                            isFree = selectedProvider.billingTier == "free",
                            label = if (selectedProvider.billingTier == "free") "FREE SLOT" else "PAID ADAPTER"
                        )
                    }
                    Text(
                        text = "Direct photorealistic 3D camera rigs, volumetric lighting, and 4s–60s shot packets. Drag inside the 3D stage below to orbit depth layers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Cinematic3DSceneStage(
                        sceneTitle = selectedScene?.title ?: "3D Shot Pre-Visualization",
                        sceneNumber = selectedScene?.sceneNumber?.toInt() ?: 1,
                        totalScenes = scenes.size.coerceAtLeast(1),
                        sceneDescription = promptText.ifBlank {
                            selectedScene?.sceneDescription ?: "Cosmic mythological temple sanctuary in photorealistic 3D depth"
                        },
                        cameraDirection = cameraRig3D,
                        lightingMood = lightingMood3D,
                        renderStyle3D = renderStyle3D,
                        subtitleText = selectedScene?.subtitleText ?: "",
                        aspectRatio = aspectRatio,
                        isPlaying = isPreviewAnimating,
                        referenceImagePath = referenceImagePath,
                        characterImagePath = primaryCharWithImage?.localImagePath.orEmpty(),
                        characterLabel = primaryCharWithImage?.name.orEmpty()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration: ${durationSeconds}s • $aspectRatio",
                            style = MaterialTheme.typography.labelMedium,
                            color = RadiantGold
                        )
                        OutlinedButton(
                            onClick = { isPreviewAnimating = !isPreviewAnimating },
                            border = BorderStroke(1.dp, ObsidianOutlineGold),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPreviewAnimating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = RadiantGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isPreviewAnimating) "Pause 3D Motion" else "Play 3D Motion",
                                style = MaterialTheme.typography.labelMedium,
                                color = RadiantGold
                            )
                        }
                    }
                }
            }
        }

        // Mode & Scene Import
        item {
            SacredCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Generation Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = RadiantGold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = mode == "text_to_video",
                            onClick = { mode = "text_to_video" },
                            label = { Text("Text-to-Video") },
                            leadingIcon = {
                                Icon(Icons.Filled.TextFields, null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                selectedLabelColor = RadiantGold
                            )
                        )
                        FilterChip(
                            selected = mode == "image_to_video",
                            onClick = { mode = "image_to_video" },
                            label = { Text("Image-to-Video") },
                            leadingIcon = {
                                Icon(Icons.Filled.Image, null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                selectedLabelColor = RadiantGold
                            )
                        )
                    }

                    if (scenes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Load From Planned Scene",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scenes.forEach { scene ->
                                    FilterChip(
                                        selected = selectedSceneId == scene.id,
                                        onClick = {
                                            selectedSceneId = scene.id
                                            durationSeconds = scene.durationSeconds.coerceIn(4L, 60L)
                                            promptText = "${scene.sceneDescription} Camera: ${scene.cameraDirection}. Actions: ${scene.characterActions}"
                                        },
                                        label = { Text("Scene ${scene.sceneNumber}: ${scene.title}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                            selectedLabelColor = RadiantGold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Duration (4s - 60s)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clip Duration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${durationSeconds}s / 60s max",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RadiantGold
                            )
                        }
                        Slider(
                            value = durationSeconds.toFloat(),
                            onValueChange = { durationSeconds = it.roundToLong().coerceIn(4L, 60L) },
                            valueRange = 4f..60f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_duration_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SacredGold,
                                activeTrackColor = SacredGold
                            )
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4L, 8L, 15L, 30L, 45L, 60L).forEach { sec ->
                                FilterChip(
                                    selected = durationSeconds == sec,
                                    onClick = { durationSeconds = sec },
                                    label = { Text("${sec}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    // Aspect Ratio
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Aspect Ratio",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("16:9", "9:16").forEach { ratio ->
                                FilterChip(
                                    selected = aspectRatio == ratio,
                                    onClick = { aspectRatio = ratio },
                                    label = { Text(ratio) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    // 3D Realism & Cinematography Rig
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "3D Render Engine",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Cinematic3DPresets.renderEngines.forEach { engine ->
                                FilterChip(
                                    selected = renderStyle3D == engine,
                                    onClick = { renderStyle3D = engine },
                                    label = { Text(engine) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "3D Camera Rig & Lens",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Cinematic3DPresets.cameraRigs.forEach { rig ->
                                FilterChip(
                                    selected = cameraRig3D == rig,
                                    onClick = { cameraRig3D = rig },
                                    label = { Text(rig) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Volumetric Lighting & Atmosphere",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Cinematic3DPresets.lightingMoods.forEach { mood ->
                                FilterChip(
                                    selected = lightingMood3D == mood,
                                    onClick = { lightingMood3D = mood },
                                    label = { Text(mood) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    // Character Reference & Keyframe Upload
                    if (characters.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Character References",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                characters.forEach { ch ->
                                    val chosen = ch.id in selectedCharIds
                                    FilterChip(
                                        selected = chosen,
                                        onClick = {
                                            selectedCharIds = if (chosen) selectedCharIds - ch.id else selectedCharIds + ch.id
                                            if (!chosen && ch.localImagePath.isNotBlank() && referenceImagePath.isBlank()) {
                                                referenceImagePath = ch.localImagePath
                                            }
                                        },
                                        label = { Text(ch.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                            selectedLabelColor = RadiantGold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, ObsidianOutlineGold)
                        ) {
                            Icon(Icons.Filled.Image, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (mode == "image_to_video") "Upload Source Frame" else "Upload Reference Image")
                        }
                        if (referenceImagePath.isNotBlank() && File(referenceImagePath).exists()) {
                            AsyncImage(
                                model = File(referenceImagePath),
                                contentDescription = "Selected video reference frame",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("3D Video Shot Prompt") },
                        placeholder = { Text("Describe 3D depth, PBR textures, camera arc, volumetric lighting, and character action...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        colors = bhaktiTextFieldColors()
                    )

                    OutlinedButton(
                        onClick = {
                            val chars = characters.filter { it.id in selectedCharIds }
                            onRefinePrompt(
                                promptText,
                                mode,
                                aspectRatio,
                                durationSeconds,
                                chars,
                                renderStyle3D,
                                cameraRig3D,
                                lightingMood3D
                            ) { refined ->
                                promptText = refined
                            }
                        },
                        enabled = videoAiState !is AiOperationState.Working,
                        border = BorderStroke(1.dp, SacredGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (videoAiState is AiOperationState.Working) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(videoAiState.label)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, tint = SacredGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Polish 3D Cinema Prompt with Gemini (Free)", color = RadiantGold)
                        }
                    }
                }
            }
        }

        // Video Model & Provider Modular Registry
        item {
            SacredCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select Video Model / Provider Slot",
                        style = MaterialTheme.typography.titleMedium,
                        color = RadiantGold
                    )
                    VideoProviderRegistry.providers.forEach { provider ->
                        val isSelected = provider.id == selectedProviderId
                        Surface(
                            onClick = { selectedProviderId = provider.id },
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) ObsidianSurfaceHigh else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SacredGold else ObsidianOutline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = provider.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) RadiantGold else MaterialTheme.colorScheme.onSurface
                                    )
                                    ServiceTierBadge(
                                        isFree = provider.billingTier == "free",
                                        label = when {
                                            provider.billingTier == "free" -> "FREE • READY"
                                            provider.isConnected -> "PAID • CONNECTED"
                                            else -> "PAID • NOT CONNECTED"
                                        }
                                    )
                                }
                                Text(
                                    text = provider.modelLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (provider.billingTier == "free") EmeraldFree else AmberPaid
                                )
                                Text(
                                    text = provider.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!selectedProvider.isConnected) {
                        Surface(
                            color = AmberPaidContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, AmberPaid.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = AmberPaid,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "No external video rendering API is connected for '${selectedProvider.displayName}'. Paid billing is never activated automatically. Submitting saves an authentic 'Awaiting Provider' render specification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val charNames = characters.filter { it.id in selectedCharIds }.map { it.name }
                            val enrichedPrompt = if (promptText.isBlank()) {
                                ""
                            } else if (renderStyle3D in promptText) {
                                promptText
                            } else {
                                "$promptText [3D Specs: $renderStyle3D | $cameraRig3D | $lightingMood3D]"
                            }
                            onQueueRenderJob(
                                selectedProvider,
                                mode,
                                enrichedPrompt,
                                aspectRatio,
                                durationSeconds,
                                selectedSceneId,
                                charNames,
                                referenceImagePath
                            )
                            if (selectedProvider.id == "gemini_omni_adapter" &&
                                selectedProvider.isConnected &&
                                enrichedPrompt.isNotBlank() &&
                                !isRendering
                            ) {
                                scope.launch {
                                    isRendering = true
                                    val ref = referenceImagePath.takeIf { it.isNotBlank() }?.let { File(it) }
                                    val out = File(context.filesDir, "bhakti_media/renders/omni_${System.currentTimeMillis()}.mp4")
                                    val ratio = if (aspectRatio == "9:16") "9:16" else "16:9"
                                    val result = GeminiOmniVideoAdapter(BuildConfig.GEMINI_API_KEY).generate(
                                        prompt = "$enrichedPrompt\n\nTarget clip length: about $durationSeconds seconds.",
                                        aspectRatio = ratio,
                                        resolution = "720p",
                                        referenceImage = ref,
                                        outFile = out
                                    )
                                    result.onSuccess { file ->
                                        val saved = RenderSaver.saveVideoToMovies(context, file, file.name)
                                        val msg = if (saved != null) {
                                            "Video saved to Movies/BhaktiMotionAI."
                                        } else {
                                            "Video saved in app storage: ${file.absolutePath}"
                                        }
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                    }.onFailure { e ->
                                        android.widget.Toast.makeText(
                                            context,
                                            e.message ?: "Video render failed.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    isRendering = false
                                }
                            }
                        },
                        enabled = !isRendering,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("queue_render_spec_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isRendering -> "Rendering… this can take a few minutes"
                                selectedProvider.id == "gemini_omni_adapter" && selectedProvider.isConnected ->
                                    "Render Video with Gemini Omni (paid API)"
                                selectedProvider.isConnected -> "Compile Free Shot Packet"
                                else -> "Queue Modular Specification"
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Authentic Generation Status List
        item {
            Text(
                text = "Render Queue & Provider Status (${jobs.size})",
                style = MaterialTheme.typography.headlineSmall,
                color = RadiantGold
            )
        }

        if (jobs.isEmpty()) {
            item {
                SacredCard {
                    Text(
                        text = "No render specifications queued yet. Compile a free Production Shot Packet or queue a modular specification above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(jobs, key = { it.id }) { job ->
                SacredCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ServiceTierBadge(
                                isFree = job.status == "packet_ready",
                                label = when (job.status) {
                                    "packet_ready" -> "PACKET READY (FREE)"
                                    "cancelled" -> "CANCELLED"
                                    else -> "AWAITING PROVIDER"
                                }
                            )
                            Row {
                                if (job.status != "cancelled") {
                                    IconButton(onClick = { onCancelJob(job.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = "Cancel specification",
                                            tint = AmberPaid
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteJob(job.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete specification",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${job.modelName} • ${job.mode.replace('_', ' ')} • ${job.aspectRatio} • ${job.durationSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = RadiantGold
                        )
                        Text(
                            text = job.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GoldAccentDivider()
                        Text(
                            text = job.statusDetail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
