package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ModularRenderJob
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BhaktiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProjectWithScenes_authenticatedOwner_persistsAndObserves() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = BhaktiFirestoreRepository(firestore)

        val project = MovieProject(
            title = "Ganga Avataran ${UUID.randomUUID().toString().take(6)}",
            storyInput = "King Bhagiratha prays for Ganga to descend from the heavens.",
            language = "sanskrit",
            visualStyle = "Sacred Watercolor Cinema",
            continuitySummary = "Cool Himalayan blue twilight warmed by golden mantra glow."
        )
        val scenes = listOf(
            MovieScene(
                sceneNumber = 1,
                title = "Tapasya in the Peaks",
                durationSeconds = 8,
                sceneDescription = "Bhagiratha meditates amid snow-clad Himalayan spires.",
                cameraDirection = "Slow crane descent toward golden aura",
                characterActions = "Eyes closed, hands folded in Anjali mudra",
                continuityNotes = "Snowfall intensity stays calm.",
                narrationScript = "गंगे च यमुने चैव गोदावरि सरस्वति।",
                sanskritPhonetics = "gaṅ-ge ca ya-mu-ne cai-va",
                subtitleText = "Sacred rivers invoked in devotion."
            )
        )

        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveProjectWithScenes(aliceUid, project, scenes).getOrThrow()
        }
        assertTrue(projectId.isNotBlank())

        val observedProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProjects(aliceUid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(observedProjects.any { it.id == projectId })

        val observedScenes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeScenes(aliceUid, projectId).first { it.isNotEmpty() }
        }
        assertEquals(1, observedScenes.size)
        assertEquals("Tapasya in the Peaks", observedScenes.first().title)
    }

    @Test
    fun saveCharacterAndRenderJob_authenticatedOwner_succeedsAndRespectsTerminalLock() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = BhaktiFirestoreRepository(firestore)

        val charId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveCharacter(
                aliceUid,
                MythCharacter(
                    name = "Lord Shiva",
                    archetype = "Adi Yogi",
                    visualDetails = "Crescent moon on matted locks, serene gaze",
                    costumeDetails = "Tiger skin wrap, rudraksha malas, trishul",
                    auraAndPalette = "Deep indigo and luminous silver-gold",
                    continuityTags = listOf("crescent_moon", "rudraksha")
                )
            ).getOrThrow()
        }
        assertTrue(charId.isNotBlank())

        val jobId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.queueRenderJob(
                aliceUid,
                ModularRenderJob(
                    projectId = "proj_test",
                    sceneId = "scene_test",
                    mode = "text_to_video",
                    providerId = "veo_adapter",
                    modelName = "External Video API (Not Connected)",
                    billingTier = "paid",
                    prompt = "Celestial Kailash twilight",
                    aspectRatio = "16:9",
                    durationSeconds = 8,
                    characterNames = listOf("Lord Shiva"),
                    status = "awaiting_provider",
                    statusDetail = "Provider not connected; billing disabled."
                )
            ).getOrThrow()
        }
        assertTrue(jobId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.cancelRenderJob(aliceUid, jobId).getOrThrow()
        }

        // Terminal state lock prevents modifying cancelled render job
        val secondUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.cancelRenderJob(aliceUid, jobId)
        }
        assertTrue(secondUpdate.isFailure)
        val firestoreErr = secondUpdate.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)
    }

    @Test
    fun crossUserAccess_readingOtherUsersProjects_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = BhaktiFirestoreRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createProject(
                aliceUid,
                MovieProject(
                    title = "Alice Private Epic",
                    storyInput = "Private story text"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProjects(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun unauthenticatedUser_observingCharacters_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BhaktiFirestoreRepository(firestore)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCharacters("some_uid").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_bhakti@test.com"
        const val BOB_EMAIL = "bob_bhakti@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
