package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BhaktiAiService
import com.example.data.ai.CharacterVisionExtraction
import com.example.data.model.ModularRenderJob
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.example.data.model.SanskritCoachResult
import com.example.data.model.StoryStarter
import com.example.data.model.VideoProviderSpec
import com.example.data.repository.BhaktiFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

sealed interface AiOperationState {
    data object Idle : AiOperationState
    data class Working(val label: String) : AiOperationState
    data class Error(val message: String) : AiOperationState
}

@OptIn(ExperimentalCoroutinesApi::class)
class BhaktiStudioViewModel(
    private val repository: BhaktiFirestoreRepository,
    private val aiService: BhaktiAiService,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val projectsState: StateFlow<DataState<List<MovieProject>>> =
        repository.observeProjects(currentUserId)
            .map<List<MovieProject>, DataState<List<MovieProject>>> { list ->
                if (_selectedProjectId.value == null && list.isNotEmpty()) {
                    _selectedProjectId.value = list.first().id
                }
                DataState.Success(list)
            }
            .catch { error ->
                val root = unwrapFirestoreOrThrow(error)
                Log.w(TAG, "Error observing projects", root)
                emit(DataState.Error(root.message ?: "Failed to load projects"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataState.Loading
            )

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    val activeProject: StateFlow<MovieProject?> =
        combine(projectsState, _selectedProjectId) { state, selectedId ->
            val list = (state as? DataState.Success)?.data ?: return@combine null
            list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    val scenesState: StateFlow<DataState<List<MovieScene>>> =
        _selectedProjectId.flatMapLatest { projId ->
            if (projId.isNullOrBlank()) {
                flowOf(DataState.Success(emptyList()))
            } else {
                repository.observeScenes(currentUserId, projId)
                    .map<List<MovieScene>, DataState<List<MovieScene>>> { DataState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreOrThrow(error)
                        Log.w(TAG, "Error observing scenes", root)
                        emit(DataState.Error(root.message ?: "Failed to load scenes"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataState.Loading
        )

    val charactersState: StateFlow<DataState<List<MythCharacter>>> =
        repository.observeCharacters(currentUserId)
            .map<List<MythCharacter>, DataState<List<MythCharacter>>> { DataState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreOrThrow(error)
                Log.w(TAG, "Error observing characters", root)
                emit(DataState.Error(root.message ?: "Failed to load characters"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataState.Loading
            )

    val renderJobsState: StateFlow<DataState<List<ModularRenderJob>>> =
        repository.observeRenderJobs(currentUserId)
            .map<List<ModularRenderJob>, DataState<List<ModularRenderJob>>> { DataState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreOrThrow(error)
                Log.w(TAG, "Error observing render jobs", root)
                emit(DataState.Error(root.message ?: "Failed to load render jobs"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataState.Loading
            )

    // Director draft state
    private val _draftTitle = MutableStateFlow("")
    val draftTitle: StateFlow<String> = _draftTitle.asStateFlow()

    private val _draftStory = MutableStateFlow("")
    val draftStory: StateFlow<String> = _draftStory.asStateFlow()

    private val _draftLanguage = MutableStateFlow("hindi")
    val draftLanguage: StateFlow<String> = _draftLanguage.asStateFlow()

    private val _draftVisualStyle = MutableStateFlow("Epic 3D Mythological")
    val draftVisualStyle: StateFlow<String> = _draftVisualStyle.asStateFlow()

    private val _attachedCharacterIds = MutableStateFlow<Set<String>>(emptySet())
    val attachedCharacterIds: StateFlow<Set<String>> = _attachedCharacterIds.asStateFlow()

    private val _directorAiState = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    val directorAiState: StateFlow<AiOperationState> = _directorAiState.asStateFlow()

    private val _characterAiState = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    val characterAiState: StateFlow<AiOperationState> = _characterAiState.asStateFlow()

    private val _voiceAiState = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    val voiceAiState: StateFlow<AiOperationState> = _voiceAiState.asStateFlow()

    private val _sanskritCoachResult = MutableStateFlow<SanskritCoachResult?>(null)
    val sanskritCoachResult: StateFlow<SanskritCoachResult?> = _sanskritCoachResult.asStateFlow()

    private val _videoAiState = MutableStateFlow<AiOperationState>(AiOperationState.Idle)
    val videoAiState: StateFlow<AiOperationState> = _videoAiState.asStateFlow()

    fun updateDraftTitle(value: String) { _draftTitle.value = value }
    fun updateDraftStory(value: String) { _draftStory.value = value }
    fun updateDraftLanguage(value: String) { _draftLanguage.value = value }
    fun updateDraftVisualStyle(value: String) { _draftVisualStyle.value = value }

    fun toggleCharacterAttachment(characterId: String) {
        val current = _attachedCharacterIds.value.toMutableSet()
        if (!current.add(characterId)) current.remove(characterId)
        _attachedCharacterIds.value = current
    }

    fun applyStoryStarter(starter: StoryStarter) {
        _selectedProjectId.value = null
        _draftTitle.value = starter.title
        _draftStory.value = starter.storyText
        _draftLanguage.value = starter.language
        _draftVisualStyle.value = starter.visualStyle
        _directorAiState.value = AiOperationState.Idle
    }

    fun startNewProjectDraft() {
        _selectedProjectId.value = null
        _draftTitle.value = ""
        _draftStory.value = ""
        _directorAiState.value = AiOperationState.Idle
    }

    fun selectProject(project: MovieProject) {
        _selectedProjectId.value = project.id
        _draftTitle.value = project.title
        _draftStory.value = project.storyInput
        _draftLanguage.value = project.language
        _draftVisualStyle.value = project.visualStyle
        _attachedCharacterIds.value = project.attachedCharacterIds.toSet()
        _directorAiState.value = AiOperationState.Idle
    }

    fun clearDirectorError() {
        if (_directorAiState.value is AiOperationState.Error) {
            _directorAiState.value = AiOperationState.Idle
        }
    }

    fun generateScenePlanWithGemini() {
        val title = _draftTitle.value.trim().ifBlank { "Sacred Chronicle" }
        val story = _draftStory.value.trim()
        if (story.length < 15) {
            _directorAiState.value = AiOperationState.Error("Please enter a story or script (at least 15 characters).")
            return
        }

        val allCharacters = (charactersState.value as? DataState.Success)?.data ?: emptyList()
        val attached = allCharacters.filter { it.id in _attachedCharacterIds.value }

        viewModelScope.launch {
            _directorAiState.value = AiOperationState.Working("Gemini is directing scenes & continuity...")
            try {
                val plan = aiService.planMovieScenes(
                    title = title,
                    storyInput = story,
                    language = _draftLanguage.value,
                    visualStyle = _draftVisualStyle.value,
                    characters = attached
                )
                val existingProject = activeProject.value?.takeIf { it.id == _selectedProjectId.value }
                val projectToSave = (existingProject ?: MovieProject()).copy(
                    title = title,
                    storyInput = story,
                    language = _draftLanguage.value,
                    visualStyle = _draftVisualStyle.value,
                    continuitySummary = plan.continuitySummary,
                    attachedCharacterIds = _attachedCharacterIds.value.toList(),
                    sceneCount = plan.scenes.size.toLong()
                )
                val savedId = repository.saveProjectWithScenes(
                    userId = currentUserId,
                    project = projectToSave,
                    scenes = plan.scenes
                ).getOrThrow()
                _selectedProjectId.value = savedId
                _directorAiState.value = AiOperationState.Idle
                _events.tryEmit("Planned ${plan.scenes.size} scenes and saved to cloud studio.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Director generation failed", e)
                _directorAiState.value = AiOperationState.Error(
                    e.localizedMessage ?: "Failed to generate scene plan."
                )
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val result = repository.deleteProject(currentUserId, projectId)
            if (result.isSuccess) {
                if (_selectedProjectId.value == projectId) {
                    _selectedProjectId.value = null
                    _draftTitle.value = ""
                    _draftStory.value = ""
                }
                _events.tryEmit("Project deleted.")
            } else {
                _events.tryEmit("Failed to delete project.")
            }
        }
    }

    fun updateScene(scene: MovieScene) {
        val projId = scene.projectId.ifBlank { _selectedProjectId.value.orEmpty() }
        if (projId.isBlank()) return
        viewModelScope.launch {
            val res = repository.upsertScene(currentUserId, projId, scene)
            if (res.isSuccess) {
                _events.tryEmit("Scene ${scene.sceneNumber} updated.")
            } else {
                _events.tryEmit("Could not save scene changes.")
            }
        }
    }

    fun moveSceneOrder(sceneId: String, direction: Int) {
        val projId = _selectedProjectId.value ?: return
        val currentScenes = (scenesState.value as? DataState.Success)?.data?.toMutableList() ?: return
        val idx = currentScenes.indexOfFirst { it.id == sceneId }
        val targetIdx = idx + direction
        if (idx < 0 || targetIdx !in currentScenes.indices) return
        val moved = currentScenes.removeAt(idx)
        currentScenes.add(targetIdx, moved)
        viewModelScope.launch {
            repository.reorderScenes(currentUserId, projId, currentScenes)
        }
    }

    fun toggleSceneIncludedInCut(scene: MovieScene) {
        updateScene(scene.copy(includedInCut = !scene.includedInCut))
    }

    fun deleteScene(scene: MovieScene) {
        val projId = scene.projectId.ifBlank { _selectedProjectId.value.orEmpty() }
        if (projId.isBlank() || scene.id.isBlank()) return
        viewModelScope.launch {
            repository.deleteScene(currentUserId, projId, scene.id)
        }
    }

    // Character Library
    fun saveCharacter(character: MythCharacter) {
        viewModelScope.launch {
            val res = repository.saveCharacter(currentUserId, character)
            if (res.isSuccess) {
                _events.tryEmit("Character '${character.name}' saved.")
            } else {
                _events.tryEmit("Failed to save character.")
            }
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            val res = repository.deleteCharacter(currentUserId, characterId)
            if (res.isSuccess) {
                _attachedCharacterIds.value = _attachedCharacterIds.value - characterId
                _events.tryEmit("Character removed.")
            }
        }
    }

    fun analyzeCharacterBitmap(
        bitmap: Bitmap,
        onExtracted: (CharacterVisionExtraction) -> Unit
    ) {
        viewModelScope.launch {
            _characterAiState.value = AiOperationState.Working("Analyzing costume & visual details...")
            try {
                val extracted = aiService.extractCharacterFromImage(bitmap)
                _characterAiState.value = AiOperationState.Idle
                onExtracted(extracted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _characterAiState.value = AiOperationState.Error(
                    e.localizedMessage ?: "Vision analysis failed."
                )
            }
        }
    }

    fun clearCharacterAiError() {
        _characterAiState.value = AiOperationState.Idle
    }

    // Voice Studio
    fun coachSanskritNarration(text: String, language: String) {
        if (text.isBlank()) {
            _voiceAiState.value = AiOperationState.Error("Enter narration text or select a scene first.")
            return
        }
        viewModelScope.launch {
            _voiceAiState.value = AiOperationState.Working("Generating Sanskrit phonetics & pacing...")
            try {
                val res = aiService.coachSanskritAndVoice(text, language)
                _sanskritCoachResult.value = res
                _voiceAiState.value = AiOperationState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _voiceAiState.value = AiOperationState.Error(
                    e.localizedMessage ?: "Voice analysis failed."
                )
            }
        }
    }

    fun attachAudioToScene(scene: MovieScene, fileName: String, localPath: String, durationMs: Long) {
        updateScene(
            scene.copy(
                audioFileName = fileName,
                audioLocalUri = localPath,
                audioDurationMs = durationMs
            )
        )
        _events.tryEmit("Attached audio '$fileName' to Scene ${scene.sceneNumber}.")
    }

    // Modular Video Studio
    fun refineShotPromptWithGemini(
        draftPrompt: String,
        mode: String,
        aspectRatio: String,
        durationSeconds: Long,
        selectedCharacters: List<MythCharacter>,
        renderStyle3D: String = "Unreal 5 Photorealistic 3D",
        cameraRig3D: String = "Orbital 3D Arc (35mm)",
        lightingMood3D: String = "Volumetric God Rays",
        onRefined: (String) -> Unit
    ) {
        if (draftPrompt.isBlank()) {
            _videoAiState.value = AiOperationState.Error("Enter a scene description or select a scene.")
            return
        }
        viewModelScope.launch {
            _videoAiState.value = AiOperationState.Working("Refining 3D cinematic shot prompt...")
            try {
                val refined = aiService.optimizeVideoPrompt(
                    basePrompt = draftPrompt,
                    mode = mode,
                    aspectRatio = aspectRatio,
                    durationSeconds = durationSeconds,
                    characters = selectedCharacters,
                    renderStyle3D = renderStyle3D,
                    cameraRig3D = cameraRig3D,
                    lightingMood3D = lightingMood3D
                )
                _videoAiState.value = AiOperationState.Idle
                onRefined(refined)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _videoAiState.value = AiOperationState.Error(
                    e.localizedMessage ?: "Failed to refine prompt."
                )
            }
        }
    }

    fun queueModularRenderJob(
        provider: VideoProviderSpec,
        mode: String,
        prompt: String,
        aspectRatio: String,
        durationSeconds: Long,
        sceneId: String,
        characterNames: List<String>,
        referenceImagePath: String
    ) {
        if (prompt.isBlank()) {
            _videoAiState.value = AiOperationState.Error("Shot prompt cannot be empty.")
            return
        }
        val status = if (provider.isConnected) "packet_ready" else "awaiting_provider"
        val detail = if (provider.isConnected && provider.billingTier != "free") {
            "Render request sent to ${provider.displayName}. The finished video is saved to Movies/BhaktiMotionAI on this device."
        } else if (provider.isConnected) {
            "Free storyboard shot packet compiled & saved. Ready for export or external adapter."
        } else {
            "${provider.displayName} is not connected. Paid billing is disabled; job queued as modular specification only."
        }
        viewModelScope.launch {
            val job = ModularRenderJob(
                projectId = _selectedProjectId.value.orEmpty(),
                sceneId = sceneId,
                mode = mode,
                providerId = provider.id,
                modelName = provider.modelLabel,
                billingTier = provider.billingTier,
                prompt = prompt,
                aspectRatio = aspectRatio,
                durationSeconds = durationSeconds,
                characterNames = characterNames,
                referenceImagePath = referenceImagePath,
                status = status,
                statusDetail = detail
            )
            val res = repository.queueRenderJob(currentUserId, job)
            if (res.isSuccess) {
                _events.tryEmit(
                    if (provider.isConnected && provider.billingTier != "free") "Render request recorded."
                    else if (provider.isConnected) "Saved free Production Shot Packet."
                    else "Queued modular specification (provider not connected)."
                )
            } else {
                _events.tryEmit("Failed to save render specification.")
            }
        }
    }

    fun cancelRenderJob(jobId: String) {
        viewModelScope.launch {
            repository.cancelRenderJob(currentUserId, jobId)
        }
    }

    fun deleteRenderJob(jobId: String) {
        viewModelScope.launch {
            repository.deleteRenderJob(currentUserId, jobId)
        }
    }

    private fun unwrapFirestoreOrThrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "BhaktiStudioVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
