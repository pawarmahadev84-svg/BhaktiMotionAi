package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.example.data.model.StoryStarters
import com.example.ui.components.GoldAccentDivider
import com.example.ui.components.SacredCard
import com.example.ui.components.ServiceTierBadge
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianOutlineGold
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SaffronEmber
import com.example.ui.viewmodel.AiOperationState
import com.example.ui.viewmodel.DataState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectorScreen(
    projectsState: DataState<List<MovieProject>>,
    activeProject: MovieProject?,
    scenesState: DataState<List<MovieScene>>,
    charactersState: DataState<List<MythCharacter>>,
    draftTitle: String,
    draftStory: String,
    draftLanguage: String,
    draftVisualStyle: String,
    attachedCharacterIds: Set<String>,
    directorAiState: AiOperationState,
    onTitleChange: (String) -> Unit,
    onStoryChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onVisualStyleChange: (String) -> Unit,
    onToggleCharacter: (String) -> Unit,
    onSelectProject: (MovieProject) -> Unit,
    onNewProjectDraft: () -> Unit,
    onApplyStarter: (com.example.data.model.StoryStarter) -> Unit,
    onGenerateScenes: () -> Unit,
    onDeleteProject: (String) -> Unit,
    onUpdateScene: (MovieScene) -> Unit
) {
    var editingScene by remember { mutableStateOf<MovieScene?>(null) }
    var projectToDelete by remember { mutableStateOf<MovieProject?>(null) }

    val projects = (projectsState as? DataState.Success)?.data ?: emptyList()
    val scenes = (scenesState as? DataState.Success)?.data ?: emptyList()
    val characters = (charactersState as? DataState.Success)?.data ?: emptyList()
    val isWorking = directorAiState is AiOperationState.Working

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "Sacred cinema studio stage",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ObsidianBackground.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ServiceTierBadge(isFree = true, label = "FREE • GEMINI AI DIRECTOR")
                    Text(
                        text = "AI Movie Director",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RadiantGold
                    )
                    Text(
                        text = "Turn sacred epics & scripts into continuous cinematic scenes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Project Selector Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Studio Projects",
                    style = MaterialTheme.typography.titleSmall,
                    color = RadiantGold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = activeProject == null,
                        onClick = onNewProjectDraft,
                        label = { Text("New Story") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                            selectedLabelColor = RadiantGold,
                            selectedLeadingIconColor = RadiantGold
                        )
                    )
                    projects.forEach { proj ->
                        val isSelected = activeProject?.id == proj.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectProject(proj) },
                            label = {
                                Text(
                                    text = "${proj.title} (${proj.sceneCount})",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete project",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { projectToDelete = proj }
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                selectedLabelColor = RadiantGold,
                                selectedTrailingIconColor = RadiantGold
                            )
                        )
                    }
                }
            }
        }

        // Story & Script Input Card
        item {
            SacredCard(goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Story & Script Input",
                            style = MaterialTheme.typography.titleLarge,
                            color = RadiantGold
                        )
                        Text(
                            text = "Step 1",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronEmber
                        )
                    }

                    // Optional Story Starters
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Quick Mythological Starters (Optional):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StoryStarters.presets.forEach { starter ->
                                OutlinedButton(
                                    onClick = { onApplyStarter(starter) },
                                    border = BorderStroke(1.dp, ObsidianOutlineGold),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = starter.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RadiantGold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = onTitleChange,
                        label = { Text("Movie / Episode Title") },
                        placeholder = { Text("e.g., Samudra Manthan: Rise of Amrita") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("director_title_input"),
                        colors = bhaktiTextFieldColors()
                    )

                    // Language Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Narration Language",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "hindi" to "Hindi",
                                "sanskrit" to "Sanskrit",
                                "english" to "English",
                                "bilingual" to "Hindi + Eng Sub"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = draftLanguage == key,
                                    onClick = { onLanguageChange(key) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    // Visual Style Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Animation Aesthetic",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Epic 3D Mythological",
                                "Photorealistic 3D Cinema",
                                "Unreal 5 Mythic 3D",
                                "Sacred Watercolor Cinema",
                                "Traditional Pattachitra-3D",
                                "Celestial Realism"
                            ).forEach { styleName ->
                                FilterChip(
                                    selected = draftVisualStyle == styleName,
                                    onClick = { onVisualStyleChange(styleName) },
                                    label = { Text(styleName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SacredGold.copy(alpha = 0.22f),
                                        selectedLabelColor = RadiantGold
                                    )
                                )
                            }
                        }
                    }

                    // Character Continuity Picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Attach Character References (for Scene Continuity)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (characters.isEmpty()) {
                            Text(
                                text = "No saved characters yet. Add characters in the Characters tab to lock costumes & aura across scenes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                characters.forEach { character ->
                                    val attached = character.id in attachedCharacterIds
                                    FilterChip(
                                        selected = attached,
                                        onClick = { onToggleCharacter(character.id) },
                                        label = { Text(character.name) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.PersonPin,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SacredGold.copy(alpha = 0.25f),
                                            selectedLabelColor = RadiantGold,
                                            selectedLeadingIconColor = RadiantGold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = draftStory,
                        onValueChange = onStoryChange,
                        label = { Text("Complete Story or Script") },
                        placeholder = { Text("Write your mythological story, shlokas, or script scene-by-scene...") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("director_story_input"),
                        colors = bhaktiTextFieldColors()
                    )

                    if (directorAiState is AiOperationState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = directorAiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onGenerateScenes,
                        enabled = !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_scenes_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = (directorAiState as AiOperationState.Working).label,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Direct & Plan Scenes with Gemini",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Continuity Bible Banner
        if (activeProject != null && activeProject.continuitySummary.isNotBlank()) {
            item {
                SacredCard(goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Visual Continuity Bible",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold
                        )
                        Text(
                            text = activeProject.continuitySummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Scene List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planned Cinematic Scenes (${scenes.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RadiantGold
                )
            }
        }

        if (scenes.isEmpty()) {
            item {
                SacredCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mythic_stage_empty),
                            contentDescription = "Empty storyboard desk illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        Text(
                            text = "Your Storyboard Awaits",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantGold
                        )
                        Text(
                            text = "Enter a mythological story above or tap a starter, then let Gemini AI divide it into continuous scenes with camera angles, mudras, and narration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(scenes, key = { it.id.ifBlank { "${it.sceneNumber}_${it.title}" } }) { scene ->
                ScenePlanCard(
                    scene = scene,
                    onEdit = { editingScene = scene }
                )
            }
        }
    }

    editingScene?.let { target ->
        EditSceneDialog(
            scene = target,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                onUpdateScene(updated)
                editingScene = null
            }
        )
    }

    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = ObsidianSurfaceHigh,
            title = { Text("Delete Project?", color = RadiantGold) },
            text = { Text("Remove '${proj.title}' and all its planned scenes from Cloud Studio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScenePlanCard(
    scene: MovieScene,
    onEdit: () -> Unit
) {
    SacredCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = SacredGold.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, ObsidianOutlineGold)
                    ) {
                        Text(
                            text = "SCENE ${scene.sceneNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = RadiantGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = scene.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = SacredGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${scene.durationSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = SacredGold
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Scene ${scene.sceneNumber}",
                            tint = RadiantGold
                        )
                    }
                }
            }

            Text(
                text = scene.sceneDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            GoldAccentDivider()

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = SaffronEmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Camera: ${scene.cameraDirection}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (scene.characterActions.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MovieFilter,
                        contentDescription = null,
                        tint = SacredGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Actions: ${scene.characterActions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (scene.continuityNotes.isNotBlank()) {
                Text(
                    text = "Continuity: ${scene.continuityNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RadiantGold.copy(alpha = 0.85f)
                )
            }

            if (scene.narrationScript.isNotBlank()) {
                Surface(
                    color = ObsidianSurfaceHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Narration: ${scene.narrationScript}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (scene.sanskritPhonetics.isNotBlank()) {
                            Text(
                                text = "Phonetics: ${scene.sanskritPhonetics}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SacredGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSceneDialog(
    scene: MovieScene,
    onDismiss: () -> Unit,
    onSave: (MovieScene) -> Unit
) {
    var title by remember { mutableStateOf(scene.title) }
    var description by remember { mutableStateOf(scene.sceneDescription) }
    var camera by remember { mutableStateOf(scene.cameraDirection) }
    var narration by remember { mutableStateOf(scene.narrationScript) }
    var subtitle by remember { mutableStateOf(scene.subtitleText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceHigh,
        title = { Text("Edit Scene ${scene.sceneNumber}", color = RadiantGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Scene Title") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Visual Description") },
                    minLines = 2,
                    maxLines = 4,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = camera,
                    onValueChange = { camera = it },
                    label = { Text("Camera Direction") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = narration,
                    onValueChange = { narration = it },
                    label = { Text("Narration Script") },
                    minLines = 2,
                    maxLines = 3,
                    colors = bhaktiTextFieldColors()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle Text") },
                    singleLine = true,
                    colors = bhaktiTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        scene.copy(
                            title = title,
                            sceneDescription = description,
                            cameraDirection = camera,
                            narrationScript = narration,
                            subtitleText = subtitle
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun bhaktiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SacredGold,
    unfocusedBorderColor = ObsidianOutline,
    focusedLabelColor = RadiantGold,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = SacredGold
)
