package com.example.ui.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.MediaStorageHelper
import com.example.core.ScriptExportHelper
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.example.ui.components.Cinematic3DSceneStage
import com.example.ui.components.GoldAccentDivider
import com.example.ui.components.SacredCard
import com.example.ui.components.ServiceTierBadge
import com.example.ui.theme.AmberPaid
import com.example.ui.theme.AmberPaidContainer
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MovieEditorScreen(
    activeProject: MovieProject?,
    scenesState: DataState<List<MovieScene>>,
    charactersState: DataState<List<MythCharacter>>,
    onMoveScene: (sceneId: String, direction: Int) -> Unit,
    onToggleSceneIncluded: (MovieScene) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scenes = (scenesState as? DataState.Success)?.data ?: emptyList()
    val characters = (charactersState as? DataState.Success)?.data ?: emptyList()
    val includedScenes = remember(scenes) { scenes.filter { it.includedInCut }.sortedBy { it.sceneNumber } }

    var isPlayingPreview by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var musicTrackName by remember { mutableStateOf("") }
    var musicTrackPath by remember { mutableStateOf("") }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) ttsReady = true
        }
    }
    val bgPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Exception) {
            }
            try {
                if (bgPlayer.isPlaying) bgPlayer.stop()
                bgPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    val musicPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val info = MediaStorageHelper.copyAudioToAppStorage(context, uri)
                    musicTrackName = info.fileName
                    musicTrackPath = info.localPath
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(isPlayingPreview, includedScenes) {
        if (!isPlayingPreview || includedScenes.isEmpty()) {
            try {
                tts.stop()
                if (bgPlayer.isPlaying) bgPlayer.pause()
            } catch (_: Exception) {
            }
            return@LaunchedEffect
        }

        if (musicTrackPath.isNotBlank()) {
            try {
                bgPlayer.reset()
                bgPlayer.setDataSource(musicTrackPath)
                bgPlayer.isLooping = true
                bgPlayer.setVolume(0.25f, 0.25f)
                bgPlayer.prepare()
                bgPlayer.start()
            } catch (_: Exception) {
            }
        }

        for (idx in currentIndex until includedScenes.size) {
            if (!isPlayingPreview) break
            currentIndex = idx
            val scene = includedScenes[idx]
            if (ttsReady && scene.narrationScript.isNotBlank()) {
                val lang = activeProject?.language ?: "hindi"
                tts.language = if (lang == "english") Locale.ENGLISH
                else Locale.Builder().setLanguage("hi").setRegion("IN").build()
                tts.speak(scene.narrationScript, TextToSpeech.QUEUE_FLUSH, null, "cut_${scene.sceneNumber}")
            }
            delay((scene.durationSeconds.coerceIn(3L, 60L)) * 1000L)
        }
        isPlayingPreview = false
        currentIndex = 0
    }

    val currentPreviewScene = includedScenes.getOrNull(currentIndex)
    val matchedCharacter = remember(currentPreviewScene, characters, activeProject) {
        val attachedIds = activeProject?.attachedCharacterIds.orEmpty().toSet()
        val pool = if (attachedIds.isNotEmpty()) {
            characters.filter { it.id in attachedIds }
        } else {
            characters
        }
        val sceneText = "${currentPreviewScene?.title} ${currentPreviewScene?.sceneDescription} ${currentPreviewScene?.characterActions}".lowercase()
        pool.firstOrNull { ch ->
            ch.localImagePath.isNotBlank() && ch.name.lowercase() in sceneText
        } ?: pool.firstOrNull { it.localImagePath.isNotBlank() }
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
                            text = "3D Movie Editor & Preview",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RadiantGold
                        )
                        ServiceTierBadge(isFree = true, label = "3D STORYBOARD CUT")
                    }

                    // Live 3D Multi-Plane Storyboard Preview Stage
                    Cinematic3DSceneStage(
                        sceneTitle = currentPreviewScene?.title ?: "Awaiting Scenes",
                        sceneNumber = currentPreviewScene?.sceneNumber?.toInt() ?: 1,
                        totalScenes = includedScenes.size.coerceAtLeast(1),
                        sceneDescription = currentPreviewScene?.sceneDescription.orEmpty(),
                        cameraDirection = currentPreviewScene?.cameraDirection.orEmpty(),
                        lightingMood = currentPreviewScene?.continuityNotes?.ifBlank { "Volumetric God Rays" }
                            ?: "Volumetric God Rays",
                        renderStyle3D = activeProject?.visualStyle?.ifBlank { "Unreal 5 Photorealistic 3D" }
                            ?: "Unreal 5 Photorealistic 3D",
                        subtitleText = currentPreviewScene?.subtitleText?.ifBlank { currentPreviewScene.title }.orEmpty(),
                        aspectRatio = "16:9",
                        isPlaying = isPlayingPreview,
                        isEmpty = currentPreviewScene == null,
                        characterImagePath = matchedCharacter?.localImagePath.orEmpty(),
                        characterLabel = matchedCharacter?.name.orEmpty()
                    )

                    if (includedScenes.isNotEmpty()) {
                        val progress = (currentIndex + 1).toFloat() / includedScenes.size.toFloat()
                        LinearProgressIndicator(
                            progress = { if (isPlayingPreview) progress else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = SacredGold,
                            trackColor = ObsidianSurfaceHigh
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPlayingPreview) {
                                    isPlayingPreview = false
                                } else if (includedScenes.isNotEmpty()) {
                                    currentIndex = 0
                                    isPlayingPreview = true
                                }
                            },
                            enabled = includedScenes.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_movie_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingPreview) "Stop Preview" else "Preview Storyboard Movie",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { musicPicker.launch(arrayOf("audio/*")) },
                            border = BorderStroke(1.dp, ObsidianOutlineGold)
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = RadiantGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (musicTrackName.isBlank()) "Music" else "Loaded",
                                color = RadiantGold
                            )
                        }
                    }
                }
            }
        }

        // Timeline / Clip Arrangement
        item {
            Text(
                text = "Timeline Sequence (${includedScenes.size}/${scenes.size} clips in cut)",
                style = MaterialTheme.typography.headlineSmall,
                color = RadiantGold
            )
        }

        itemsIndexed(scenes, key = { _, s -> s.id.ifBlank { "${s.sceneNumber}_${s.title}" } }) { idx, scene ->
            SacredCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { onToggleSceneIncluded(scene) }) {
                        Icon(
                            imageVector = if (scene.includedInCut) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = "Include Scene ${scene.sceneNumber} in cut",
                            tint = if (scene.includedInCut) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scene ${scene.sceneNumber}: ${scene.title} (${scene.durationSeconds}s)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (scene.includedInCut) RadiantGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (scene.subtitleText.isNotBlank()) {
                            Text(
                                text = "Sub: ${scene.subtitleText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (scene.audioFileName.isNotBlank()) {
                            Text(
                                text = "Audio: ${scene.audioFileName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SacredGold
                            )
                        }
                    }
                    IconButton(
                        onClick = { onMoveScene(scene.id, -1) },
                        enabled = idx > 0
                    ) {
                        Icon(Icons.Filled.ArrowUpward, "Move scene up", tint = RadiantGold)
                    }
                    IconButton(
                        onClick = { onMoveScene(scene.id, 1) },
                        enabled = idx < scenes.lastIndex
                    ) {
                        Icon(Icons.Filled.ArrowDownward, "Move scene down", tint = RadiantGold)
                    }
                }
            }
        }

        // Export Panel (Honest Free vs Paid MP4 Render)
        item {
            SacredCard(goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Export & Production Output",
                        style = MaterialTheme.typography.titleLarge,
                        color = RadiantGold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val srt = ScriptExportHelper.buildSrtSubtitles(scenes)
                                if (srt.isNotBlank()) {
                                    ScriptExportHelper.shareTextPackage(
                                        context,
                                        "${activeProject?.title ?: "BhaktiMotion"} Subtitles (.srt)",
                                        srt
                                    )
                                }
                            },
                            enabled = includedScenes.isNotEmpty(),
                            border = BorderStroke(1.dp, SacredGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_srt_button")
                        ) {
                            Icon(Icons.Filled.Subtitles, null, tint = RadiantGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export SRT", color = RadiantGold)
                        }

                        OutlinedButton(
                            onClick = {
                                val proj = activeProject ?: return@OutlinedButton
                                val md = ScriptExportHelper.buildProductionScriptMarkdown(proj, scenes, characters)
                                ScriptExportHelper.shareTextPackage(
                                    context,
                                    "${proj.title} Production Script",
                                    md
                                )
                            },
                            enabled = activeProject != null && includedScenes.isNotEmpty(),
                            border = BorderStroke(1.dp, SacredGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_script_button")
                        ) {
                            Icon(Icons.Filled.Description, null, tint = RadiantGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export Script", color = RadiantGold)
                        }
                    }

                    GoldAccentDivider()

                    Surface(
                        color = AmberPaidContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, AmberPaid.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = AmberPaid)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Final Cloud MP4 Render — Provider Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AmberPaid
                                )
                                Text(
                                    text = "Final video assembly unlocks once an external video rendering provider is connected in Video Studio. Paid billing is never activated automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
