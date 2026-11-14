package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.MediaStorageHelper
import com.example.data.ai.CharacterVisionExtraction
import com.example.data.model.MythCharacter
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterLibraryScreen(
    charactersState: DataState<List<MythCharacter>>,
    characterAiState: AiOperationState,
    onSaveCharacter: (MythCharacter) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onAnalyzeBitmap: (Bitmap, (CharacterVisionExtraction) -> Unit) -> Unit,
    onClearAiError: () -> Unit
) {
    var editingCharacter by remember { mutableStateOf<MythCharacter?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val characters = (charactersState as? DataState.Success)?.data ?: emptyList()

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
                        Column(modifier = Modifier.weight(1f)) {
                            ServiceTierBadge(isFree = true, label = "FREE • CLOUD CONTINUITY")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Character Library",
                                style = MaterialTheme.typography.headlineMedium,
                                color = RadiantGold
                            )
                        }
                        Button(
                            onClick = {
                                onClearAiError()
                                editingCharacter = MythCharacter()
                                showEditor = true
                            },
                            modifier = Modifier.testTag("add_character_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("New", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text(
                        text = "Save mythological character reference images, attire, ornaments, and aura palettes so Gemini preserves visual consistency across every scene.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (characters.isEmpty()) {
            item {
                SacredCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_character_emblem),
                            contentDescription = "Ornate golden character medallion",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "No Characters Saved Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold
                        )
                        Text(
                            text = "Add a character manually or upload a reference image using the Photo Picker—Gemini Vision can extract costume & visual traits automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(characters, key = { it.id }) { character ->
                SacredCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val imageFile = character.localImagePath.takeIf { it.isNotBlank() }?.let { File(it) }
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                            ) {
                                if (imageFile != null && imageFile.exists()) {
                                    AsyncImage(
                                        model = imageFile,
                                        contentDescription = "${character.name} reference image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_character_emblem),
                                        contentDescription = "${character.name} emblem",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = character.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RadiantGold
                                )
                                Text(
                                    text = character.archetype,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SacredGold
                                )
                            }
                            IconButton(
                                onClick = {
                                    onClearAiError()
                                    editingCharacter = character
                                    showEditor = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit ${character.name}",
                                    tint = RadiantGold
                                )
                            }
                            IconButton(onClick = { onDeleteCharacter(character.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete ${character.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        GoldAccentDivider()

                        if (character.visualDetails.isNotBlank()) {
                            Text(
                                text = "Visuals: ${character.visualDetails}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (character.costumeDetails.isNotBlank()) {
                            Text(
                                text = "Costume & Ornaments: ${character.costumeDetails}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (character.auraAndPalette.isNotBlank()) {
                            Text(
                                text = "Aura & Palette: ${character.auraAndPalette}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SacredGold
                            )
                        }
                        if (character.continuityTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                character.continuityTags.forEach { tag ->
                                    Surface(
                                        color = ObsidianSurfaceHigh,
                                        shape = MaterialTheme.shapes.extraSmall,
                                        border = BorderStroke(1.dp, ObsidianOutlineGold)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RadiantGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor && editingCharacter != null) {
        CharacterEditorDialog(
            initial = editingCharacter!!,
            aiState = characterAiState,
            onAnalyzeBitmap = onAnalyzeBitmap,
            onDismiss = { showEditor = false },
            onSave = { updated ->
                onSaveCharacter(updated)
                showEditor = false
            }
        )
    }
}

@Composable
private fun CharacterEditorDialog(
    initial: MythCharacter,
    aiState: AiOperationState,
    onAnalyzeBitmap: (Bitmap, (CharacterVisionExtraction) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MythCharacter) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial.name) }
    var archetype by remember { mutableStateOf(initial.archetype) }
    var visualDetails by remember { mutableStateOf(initial.visualDetails) }
    var costumeDetails by remember { mutableStateOf(initial.costumeDetails) }
    var auraPalette by remember { mutableStateOf(initial.auraAndPalette) }
    var tagsCsv by remember { mutableStateOf(initial.continuityTags.joinToString(", ")) }
    var localImagePath by remember { mutableStateOf(initial.localImagePath) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = MediaStorageHelper.copyImageToAppStorage(context, uri, "characters")
                localImagePath = path
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceHigh,
        title = {
            Text(
                text = if (initial.id.isBlank()) "New Character Reference" else "Edit Character",
                color = RadiantGold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val file = localImagePath.takeIf { it.isNotBlank() }?.let { File(it) }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    ) {
                        if (file != null && file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Selected character photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_character_emblem),
                                contentDescription = "Character emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, ObsidianOutlineGold),
                            modifier = Modifier.testTag("pick_character_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Reference Image")
                        }
                        if (localImagePath.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val bmp = MediaStorageHelper.decodeBitmapForAi(context, localImagePath)
                                        if (bmp != null) {
                                            onAnalyzeBitmap(bmp) { extracted ->
                                                if (name.isBlank()) name = extracted.name
                                                if (archetype.isBlank()) archetype = extracted.archetype
                                                visualDetails = extracted.visualDetails
                                                costumeDetails = extracted.costumeDetails
                                                auraPalette = extracted.auraAndPalette
                                                tagsCsv = extracted.continuityTags.joinToString(", ")
                                            }
                                        }
                                    }
                                },
                                enabled = aiState !is AiOperationState.Working,
                                border = BorderStroke(1.dp, SacredGold)
                            ) {
                                if (aiState is AiOperationState.Working) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Analyzing...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Extract via Gemini", color = RadiantGold)
                                }
                            }
                        }
                    }
                }

                if (aiState is AiOperationState.Error) {
                    Text(
                        text = aiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Character Name") },
                    placeholder = { Text("e.g., Shri Krishna") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors(),
                    modifier = Modifier.testTag("character_name_input")
                )
                OutlinedTextField(
                    value = archetype,
                    onValueChange = { archetype = it },
                    label = { Text("Role / Archetype") },
                    placeholder = { Text("e.g., Divine Charioteer / Protector") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = visualDetails,
                    onValueChange = { visualDetails = it },
                    label = { Text("Visual & Facial Details") },
                    minLines = 2,
                    maxLines = 4,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = costumeDetails,
                    onValueChange = { costumeDetails = it },
                    label = { Text("Costume, Crown & Ornaments") },
                    minLines = 2,
                    maxLines = 4,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = auraPalette,
                    onValueChange = { auraPalette = it },
                    label = { Text("Aura & Color Palette") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text("Continuity Tags (comma-separated)") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsCsv.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .take(15)
                    onSave(
                        initial.copy(
                            name = name.ifBlank { "Unnamed Character" },
                            archetype = archetype.ifBlank { "Mythological Figure" },
                            visualDetails = visualDetails,
                            costumeDetails = costumeDetails,
                            auraAndPalette = auraPalette,
                            continuityTags = tags,
                            localImagePath = localImagePath
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                modifier = Modifier.testTag("save_character_button")
            ) {
                Text("Save Character", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
