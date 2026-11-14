package com.example.ui.screens

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.MovieScene
import com.example.data.model.SanskritCoachResult
import com.example.ui.components.GoldAccentDivider
import com.example.ui.components.SacredCard
import com.example.ui.components.ServiceTierBadge
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.DataState
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceStudioScreen(
    scenesState: DataState<List<MovieScene>>,
    voiceAiState: AiOperationState,
    sanskritCoachResult: SanskritCoachResult?,
    onCoachSanskrit: (text: String, language: String) -> Unit,
    onAttachAudioToScene: (MovieScene, fileName: String, localPath: String, durationMs: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scenes = (scenesState as? DataState.Success)?.data ?: emptyList()
    var selectedSceneId by remember { mutableStateOf("") }
    var voiceLanguage by remember { mutableStateOf("hindi") }
    var narrationInput by remember {
        mutableStateOf("ॐ नमो भगवते वासुदेवाय। क्षीर सागर की दिव्य तरंगों में शांति व्याप्त है।")
    }
    var speechRate by remember { mutableFloatStateOf(0.9f) }
    var ttsReady by remember { mutableStateOf(false) }
    var uploadedFileLabel by remember { mutableStateOf("") }
    var uploadedFilePath by remember { mutableStateOf("") }
    var isPlayingAudio by remember { mutableStateOf(false) }

    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        instance
    }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Exception) {
            }
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val info = MediaStorageHelper.copyAudioToAppStorage(context, uri)
                    uploadedFileLabel = "${info.fileName} (${info.durationMs / 1000}s)"
                    uploadedFilePath = info.localPath
                    selectedScene?.let { scene ->
                        onAttachAudioToScene(scene, info.fileName, info.localPath, info.durationMs)
                    }
                } catch (_: Exception) {
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice & Sanskrit Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RadiantGold
                        )
                        ServiceTierBadge(isFree = true, label = "FREE • ON-DEVICE + GEMINI")
                    }
                    Text(
                        text = "Craft Hindi, Sanskrit, and English narration with Gemini phonetic coaching, rehearse via Android Text-to-Speech, or upload recorded voiceovers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scene Selection & Language
        item {
            SacredCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scenes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Select Planned Scene",
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
                                            if (scene.narrationScript.isNotBlank()) {
                                                narrationInput = scene.narrationScript
                                            }
                                            if (scene.audioLocalUri.isNotBlank()) {
                                                uploadedFilePath = scene.audioLocalUri
                                                uploadedFileLabel = scene.audioFileName
                                            }
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

                    Text(
                        text = "Narration & Pronunciation Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "hindi" to "Hindi Narration",
                            "sanskrit" to "Sanskrit Shloka Support",
                            "english" to "English Narration"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = voiceLanguage == key,
                                onClick = { voiceLanguage = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                    selectedLabelColor = RadiantGold
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = narrationInput,
                        onValueChange = { narrationInput = it },
                        label = { Text("Narration Text or Sanskrit Shloka") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_narration_input"),
                        colors = bhaktiTextFieldColors()
                    )

                    // On-Device Speech Preview
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Rehearsal Pace: ${String.format("%.2fx", speechRate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.6f..1.3f,
                            colors = SliderDefaults.colors(
                                thumbColor = SacredGold,
                                activeTrackColor = SacredGold
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (ttsReady) {
                                    val locale = when (voiceLanguage) {
                                        "hindi", "sanskrit" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
                                        else -> Locale.ENGLISH
                                    }
                                    tts.language = locale
                                    tts.setSpeechRate(speechRate)
                                    tts.speak(narrationInput, TextToSpeech.QUEUE_FLUSH, null, "bhakti_preview")
                                }
                            },
                            enabled = ttsReady && narrationInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Icon(Icons.Filled.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Preview Voice", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { tts.stop() },
                            border = BorderStroke(1.dp, ObsidianOutlineGold)
                        ) {
                            Icon(Icons.Filled.Stop, null, tint = RadiantGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", color = RadiantGold)
                        }
                    }

                    OutlinedButton(
                        onClick = { onCoachSanskrit(narrationInput, voiceLanguage) },
                        enabled = voiceAiState !is AiOperationState.Working,
                        border = BorderStroke(1.dp, SacredGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sanskrit_coach_button")
                    ) {
                        if (voiceAiState is AiOperationState.Working) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(voiceAiState.label)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, tint = SacredGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Get Sanskrit Pronunciation & Delivery Guide (Gemini)", color = RadiantGold)
                        }
                    }

                    if (voiceAiState is AiOperationState.Error) {
                        Text(
                            text = voiceAiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Gemini Sanskrit Phonetics Output
        if (sanskritCoachResult != null) {
            item {
                SacredCard(goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sanskrit & Devotional Pronunciation Guide",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold
                        )
                        Text(
                            text = "Devanagari: ${sanskritCoachResult.devanagari}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GoldAccentDivider()
                        Text(
                            text = "IAST Transliteration: ${sanskritCoachResult.iastTransliteration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SacredGold
                        )
                        Text(
                            text = "Syllable Guide: ${sanskritCoachResult.syllableBreakdown}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Delivery Notes: ${sanskritCoachResult.deliveryNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Real Audio Upload Card
        item {
            SacredCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AudioFile, null, tint = SacredGold)
                        Text(
                            text = "Voiceover & Music Audio Upload",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold
                        )
                    }
                    Text(
                        text = if (selectedScene != null) {
                            "Upload a recorded narration or devotional track to attach to Scene ${selectedScene.sceneNumber}."
                        } else {
                            "Pick an audio file from your device (or select a planned scene above first to attach it)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        border = BorderStroke(1.dp, ObsidianOutlineGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_audio_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, null, tint = RadiantGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Audio File (MP3 / WAV / M4A)", color = RadiantGold)
                    }

                    if (uploadedFilePath.isNotBlank() && File(uploadedFilePath).exists()) {
                        Surface(
                            color = ObsidianSurfaceHigh,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uploadedFileLabel.ifBlank { File(uploadedFilePath).name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RadiantGold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (isPlayingAudio) {
                                            try {
                                                mediaPlayer.stop()
                                            } catch (_: Exception) {
                                            }
                                            isPlayingAudio = false
                                        } else {
                                            try {
                                                mediaPlayer.reset()
                                                mediaPlayer.setDataSource(uploadedFilePath)
                                                mediaPlayer.prepare()
                                                mediaPlayer.setOnCompletionListener { isPlayingAudio = false }
                                                mediaPlayer.start()
                                                isPlayingAudio = true
                                            } catch (_: Exception) {
                                                isPlayingAudio = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlayingAudio) "Stop" else "Play",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = ObsidianSurfaceHigh,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cloud Neural Dubbing API",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Optional studio voice synthesis adapter (not connected).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ServiceTierBadge(isFree = false, label = "PAID • OFF")
                        }
                    }
                }
            }
        }
    }
}
