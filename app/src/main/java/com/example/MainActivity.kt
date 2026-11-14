package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.configureAppCheck
import com.example.data.ai.BhaktiAiService
import com.example.data.repository.BhaktiFirestoreRepository
import com.example.ui.components.BillingTransparencySheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterLibraryScreen
import com.example.ui.screens.DirectorScreen
import com.example.ui.screens.MovieEditorScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.ui.screens.VoiceStudioScreen
import com.example.ui.theme.BhaktiMotionTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.BhaktiStudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val DIRECTOR = "director"
    const val CHARACTERS = "characters"
    const val VIDEO_STUDIO = "video_studio"
    const val VOICE_STUDIO = "voice_studio"
    const val EDITOR = "editor"
}

private data class StudioTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val studioTabs = listOf(
    StudioTab(Routes.DIRECTOR, "Director", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    StudioTab(Routes.CHARACTERS, "Cast", Icons.Filled.People, Icons.Outlined.People),
    StudioTab(Routes.VIDEO_STUDIO, "Video", Icons.Filled.Movie, Icons.Outlined.Movie),
    StudioTab(Routes.VOICE_STUDIO, "Voice", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    StudioTab(Routes.EDITOR, "Editor", Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before Compose UI / ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            BhaktiMotionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            BhaktiStudioShell(currentUserId = user.uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BhaktiStudioShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: BhaktiStudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BhaktiStudioViewModel(
                    repository = BhaktiFirestoreRepository(db),
                    aiService = BhaktiAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val scenesState by viewModel.scenesState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val renderJobsState by viewModel.renderJobsState.collectAsStateWithLifecycle()

    val draftTitle by viewModel.draftTitle.collectAsStateWithLifecycle()
    val draftStory by viewModel.draftStory.collectAsStateWithLifecycle()
    val draftLanguage by viewModel.draftLanguage.collectAsStateWithLifecycle()
    val draftVisualStyle by viewModel.draftVisualStyle.collectAsStateWithLifecycle()
    val attachedCharIds by viewModel.attachedCharacterIds.collectAsStateWithLifecycle()

    val directorAiState by viewModel.directorAiState.collectAsStateWithLifecycle()
    val characterAiState by viewModel.characterAiState.collectAsStateWithLifecycle()
    val voiceAiState by viewModel.voiceAiState.collectAsStateWithLifecycle()
    val sanskritCoachResult by viewModel.sanskritCoachResult.collectAsStateWithLifecycle()
    val videoAiState by viewModel.videoAiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showBillingSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DIRECTOR

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "BhaktiMotion AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = RadiantGold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface
                    ),
                    actions = {
                        IconButton(
                            onClick = { showBillingSheet = true },
                            modifier = Modifier.testTag("billing_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Free vs Paid Services Info",
                                tint = SacredGold
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    withContext(NonCancellable) {
                                        try {
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        } catch (e: Exception) {
                                            Log.w("Auth", "Clear credential failed: ${e.message}")
                                        } finally {
                                            Firebase.auth.signOut()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface
                    ) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.DIRECTOR) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RadiantGold,
                                    selectedTextColor = RadiantGold,
                                    indicatorColor = ObsidianSurfaceHigh,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        studioTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.DIRECTOR) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RadiantGold,
                                    selectedTextColor = RadiantGold,
                                    indicatorColor = ObsidianSurfaceHigh,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.DIRECTOR,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()
                    ) {
                        composable(Routes.DIRECTOR) {
                            DirectorScreen(
                                projectsState = projectsState,
                                activeProject = activeProject,
                                scenesState = scenesState,
                                charactersState = charactersState,
                                draftTitle = draftTitle,
                                draftStory = draftStory,
                                draftLanguage = draftLanguage,
                                draftVisualStyle = draftVisualStyle,
                                attachedCharacterIds = attachedCharIds,
                                directorAiState = directorAiState,
                                onTitleChange = viewModel::updateDraftTitle,
                                onStoryChange = viewModel::updateDraftStory,
                                onLanguageChange = viewModel::updateDraftLanguage,
                                onVisualStyleChange = viewModel::updateDraftVisualStyle,
                                onToggleCharacter = viewModel::toggleCharacterAttachment,
                                onSelectProject = viewModel::selectProject,
                                onNewProjectDraft = viewModel::startNewProjectDraft,
                                onApplyStarter = viewModel::applyStoryStarter,
                                onGenerateScenes = viewModel::generateScenePlanWithGemini,
                                onDeleteProject = viewModel::deleteProject,
                                onUpdateScene = viewModel::updateScene
                            )
                        }
                        composable(Routes.CHARACTERS) {
                            CharacterLibraryScreen(
                                charactersState = charactersState,
                                characterAiState = characterAiState,
                                onSaveCharacter = viewModel::saveCharacter,
                                onDeleteCharacter = viewModel::deleteCharacter,
                                onAnalyzeBitmap = viewModel::analyzeCharacterBitmap,
                                onClearAiError = viewModel::clearCharacterAiError
                            )
                        }
                        composable(Routes.VIDEO_STUDIO) {
                            VideoStudioScreen(
                                scenesState = scenesState,
                                charactersState = charactersState,
                                renderJobsState = renderJobsState,
                                videoAiState = videoAiState,
                                onRefinePrompt = viewModel::refineShotPromptWithGemini,
                                onQueueRenderJob = viewModel::queueModularRenderJob,
                                onCancelJob = viewModel::cancelRenderJob,
                                onDeleteJob = viewModel::deleteRenderJob
                            )
                        }
                        composable(Routes.VOICE_STUDIO) {
                            VoiceStudioScreen(
                                scenesState = scenesState,
                                voiceAiState = voiceAiState,
                                sanskritCoachResult = sanskritCoachResult,
                                onCoachSanskrit = viewModel::coachSanskritNarration,
                                onAttachAudioToScene = viewModel::attachAudioToScene
                            )
                        }
                        composable(Routes.EDITOR) {
                            MovieEditorScreen(
                                activeProject = activeProject,
                                scenesState = scenesState,
                                charactersState = charactersState,
                                onMoveScene = viewModel::moveSceneOrder,
                                onToggleSceneIncluded = viewModel::toggleSceneIncludedInCut
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBillingSheet) {
        BillingTransparencySheet(onDismiss = { showBillingSheet = false })
    }
}
