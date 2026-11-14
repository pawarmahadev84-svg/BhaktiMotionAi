package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ModularRenderJob
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BhaktiFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun projectsCol(userId: String) =
        db.collection("users").document(userId).collection("projects")

    private fun scenesCol(userId: String, projectId: String) =
        projectsCol(userId).document(projectId).collection("scenes")

    private fun charactersCol(userId: String) =
        db.collection("users").document(userId).collection("characters")

    private fun renderJobsCol(userId: String) =
        db.collection("users").document(userId).collection("renderJobs")

    fun observeProjects(userId: String): Flow<List<MovieProject>> {
        val path = "users/$userId/projects"
        return projectsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MovieProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowFailure(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeScenes(userId: String, projectId: String): Flow<List<MovieScene>> {
        val path = "users/$userId/projects/$projectId/scenes"
        return scenesCol(userId, projectId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MovieScene::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.sceneNumber }
            }
            .catch { error ->
                logFlowFailure(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeCharacters(userId: String): Flow<List<MythCharacter>> {
        val path = "users/$userId/characters"
        return charactersCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MythCharacter::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowFailure(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeRenderJobs(userId: String): Flow<List<ModularRenderJob>> {
        val path = "users/$userId/renderJobs"
        return renderJobsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ModularRenderJob::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowFailure(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createProject(userId: String, project: MovieProject): Result<String> {
        val docId = project.id.ifBlank { "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = projectsCol(userId).document(docId)
        val payload = mapOf(
            "userId" to userId,
            "title" to project.title.trim().take(120).ifBlank { "Untitled Epic" },
            "storyInput" to project.storyInput.trim().take(12000).ifBlank { " " },
            "language" to project.language.lowercase().takeIf {
                it in setOf("hindi", "sanskrit", "english", "bilingual")
            }.orEmpty().ifBlank { "hindi" },
            "visualStyle" to project.visualStyle.trim().take(80).ifBlank { "Epic 3D Mythological" },
            "continuitySummary" to project.continuitySummary.trim().take(2000),
            "attachedCharacterIds" to project.attachedCharacterIds.take(20),
            "sceneCount" to project.sceneCount.coerceIn(0L, 50L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProject(userId: String, project: MovieProject): Result<Unit> {
        require(project.id.isNotBlank()) { "Project ID is required for update" }
        val docRef = projectsCol(userId).document(project.id)
        val updates = mapOf(
            "title" to project.title.trim().take(120).ifBlank { "Untitled Epic" },
            "storyInput" to project.storyInput.trim().take(12000).ifBlank { " " },
            "language" to project.language.lowercase().takeIf {
                it in setOf("hindi", "sanskrit", "english", "bilingual")
            }.orEmpty().ifBlank { "hindi" },
            "visualStyle" to project.visualStyle.trim().take(80).ifBlank { "Epic 3D Mythological" },
            "continuitySummary" to project.continuitySummary.trim().take(2000),
            "attachedCharacterIds" to project.attachedCharacterIds.take(20),
            "sceneCount" to project.sceneCount.coerceIn(0L, 50L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveProjectWithScenes(
        userId: String,
        project: MovieProject,
        scenes: List<MovieScene>
    ): Result<String> {
        val projectIdResult = if (project.id.isBlank()) {
            createProject(userId, project.copy(sceneCount = scenes.size.toLong()))
        } else {
            updateProject(userId, project.copy(sceneCount = scenes.size.toLong()))
                .map { project.id }
        }
        val projectId = projectIdResult.getOrElse { return Result.failure(it) }

        return try {
            // Remove previous scenes so regenerating a plan replaces cleanly
            val existingDocs = scenesCol(userId, projectId).get().await()
            for (doc in existingDocs.documents) {
                doc.reference.delete().await()
            }
            for ((index, scene) in scenes.withIndex()) {
                val sceneId = "scene_${index + 1}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
                val payload = buildSceneCreatePayload(
                    userId = userId,
                    projectId = projectId,
                    scene = scene.copy(sceneNumber = (index + 1).toLong())
                )
                scenesCol(userId, projectId).document(sceneId).set(payload).await()
            }
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/projects/$projectId/scenes")
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val projRef = projectsCol(userId).document(projectId)
        return try {
            val existingScenes = scenesCol(userId, projectId).get().await()
            for (doc in existingScenes.documents) {
                doc.reference.delete().await()
            }
            projRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, projRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertScene(userId: String, projectId: String, scene: MovieScene): Result<String> {
        val isCreate = scene.id.isBlank()
        val sceneId = scene.id.ifBlank { "scene_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docRef = scenesCol(userId, projectId).document(sceneId)
        return try {
            if (isCreate) {
                docRef.set(buildSceneCreatePayload(userId, projectId, scene)).await()
            } else {
                docRef.update(buildSceneUpdatePayload(scene)).await()
            }
            Result.success(sceneId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isCreate) OperationType.CREATE else OperationType.UPDATE,
                docRef.path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteScene(userId: String, projectId: String, sceneId: String): Result<Unit> {
        val docRef = scenesCol(userId, projectId).document(sceneId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun reorderScenes(
        userId: String,
        projectId: String,
        orderedScenes: List<MovieScene>
    ): Result<Unit> {
        return try {
            val batch = db.batch()
            orderedScenes.forEachIndexed { index, scene ->
                if (scene.id.isNotBlank()) {
                    val docRef = scenesCol(userId, projectId).document(scene.id)
                    batch.update(
                        docRef,
                        mapOf(
                            "sceneNumber" to (index + 1).toLong(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/projects/$projectId/scenes")
            Result.failure(e)
        }
    }

    suspend fun saveCharacter(userId: String, character: MythCharacter): Result<String> {
        val isCreate = character.id.isBlank()
        val charId = character.id.ifBlank { "char_${UUID.randomUUID().toString().replace("-", "").take(14)}" }
        val docRef = charactersCol(userId).document(charId)
        return try {
            if (isCreate) {
                val payload = mapOf(
                    "userId" to userId,
                    "name" to character.name.trim().take(100).ifBlank { "Unnamed Character" },
                    "archetype" to character.archetype.trim().take(100).ifBlank { "Mythological Figure" },
                    "visualDetails" to character.visualDetails.trim().take(2500),
                    "costumeDetails" to character.costumeDetails.trim().take(2500),
                    "auraAndPalette" to character.auraAndPalette.trim().take(600),
                    "continuityTags" to character.continuityTags.take(15).map { it.trim().take(128) },
                    "localImagePath" to character.localImagePath.trim().take(600),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to character.name.trim().take(100).ifBlank { "Unnamed Character" },
                    "archetype" to character.archetype.trim().take(100).ifBlank { "Mythological Figure" },
                    "visualDetails" to character.visualDetails.trim().take(2500),
                    "costumeDetails" to character.costumeDetails.trim().take(2500),
                    "auraAndPalette" to character.auraAndPalette.trim().take(600),
                    "continuityTags" to character.continuityTags.take(15).map { it.trim().take(128) },
                    "localImagePath" to character.localImagePath.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(charId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isCreate) OperationType.CREATE else OperationType.UPDATE,
                docRef.path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(userId: String, characterId: String): Result<Unit> {
        val docRef = charactersCol(userId).document(characterId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun queueRenderJob(userId: String, job: ModularRenderJob): Result<String> {
        val jobId = job.id.ifBlank { "job_${UUID.randomUUID().toString().replace("-", "").take(14)}" }
        val docRef = renderJobsCol(userId).document(jobId)
        val payload = mapOf(
            "userId" to userId,
            "projectId" to job.projectId.take(128),
            "sceneId" to job.sceneId.take(128),
            "mode" to if (job.mode == "image_to_video") "image_to_video" else "text_to_video",
            "providerId" to job.providerId.trim().take(80).ifBlank { "storyboard_packet" },
            "modelName" to job.modelName.trim().take(120).ifBlank { "Modular Slot" },
            "billingTier" to if (job.billingTier == "paid") "paid" else "free",
            "prompt" to job.prompt.trim().take(4000).ifBlank { "Cinematic mythological frame" },
            "aspectRatio" to if (job.aspectRatio == "9:16") "9:16" else "16:9",
            "durationSeconds" to job.durationSeconds.coerceIn(2L, 60L),
            "characterNames" to job.characterNames.take(20).map { it.trim().take(128) },
            "referenceImagePath" to job.referenceImagePath.trim().take(600),
            "status" to job.status.takeIf {
                it in setOf("awaiting_provider", "packet_ready", "cancelled")
            }.orEmpty().ifBlank { "awaiting_provider" },
            "statusDetail" to job.statusDetail.trim().take(600),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(jobId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelRenderJob(userId: String, jobId: String): Result<Unit> {
        val docRef = renderJobsCol(userId).document(jobId)
        val updates = mapOf(
            "status" to "cancelled",
            "statusDetail" to "Cancelled by creator.",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRenderJob(userId: String, jobId: String): Result<Unit> {
        val docRef = renderJobsCol(userId).document(jobId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun buildSceneCreatePayload(
        userId: String,
        projectId: String,
        scene: MovieScene
    ): Map<String, Any> = mapOf(
        "userId" to userId,
        "projectId" to projectId,
        "sceneNumber" to scene.sceneNumber.coerceIn(1L, 100L),
        "title" to scene.title.trim().take(150).ifBlank { "Scene ${scene.sceneNumber}" },
        "durationSeconds" to scene.durationSeconds.coerceIn(2L, 60L),
        "sceneDescription" to scene.sceneDescription.trim().take(4000).ifBlank { "Cinematic devotional shot." },
        "cameraDirection" to scene.cameraDirection.trim().take(1000).ifBlank { "Wide slow push-in" },
        "characterActions" to scene.characterActions.trim().take(2000),
        "continuityNotes" to scene.continuityNotes.trim().take(1500),
        "narrationScript" to scene.narrationScript.trim().take(3000),
        "sanskritPhonetics" to scene.sanskritPhonetics.trim().take(2000),
        "subtitleText" to scene.subtitleText.trim().take(1000),
        "includedInCut" to scene.includedInCut,
        "audioFileName" to scene.audioFileName.trim().take(200),
        "audioLocalUri" to scene.audioLocalUri.trim().take(600),
        "audioDurationMs" to scene.audioDurationMs.coerceIn(0L, 3600000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun buildSceneUpdatePayload(scene: MovieScene): Map<String, Any> = mapOf(
        "sceneNumber" to scene.sceneNumber.coerceIn(1L, 100L),
        "title" to scene.title.trim().take(150).ifBlank { "Scene ${scene.sceneNumber}" },
        "durationSeconds" to scene.durationSeconds.coerceIn(2L, 60L),
        "sceneDescription" to scene.sceneDescription.trim().take(4000).ifBlank { "Cinematic devotional shot." },
        "cameraDirection" to scene.cameraDirection.trim().take(1000).ifBlank { "Wide slow push-in" },
        "characterActions" to scene.characterActions.trim().take(2000),
        "continuityNotes" to scene.continuityNotes.trim().take(1500),
        "narrationScript" to scene.narrationScript.trim().take(3000),
        "sanskritPhonetics" to scene.sanskritPhonetics.trim().take(2000),
        "subtitleText" to scene.subtitleText.trim().take(1000),
        "includedInCut" to scene.includedInCut,
        "audioFileName" to scene.audioFileName.trim().take(200),
        "audioLocalUri" to scene.audioLocalUri.trim().take(600),
        "audioDurationMs" to scene.audioDurationMs.coerceIn(0L, 3600000L),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun logFlowFailure(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception) ?: error
        handleFirestoreError(exceptionToLog, op, path)
    }
}
