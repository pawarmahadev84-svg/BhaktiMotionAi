package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import com.example.data.model.SanskritCoachResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class PlannedMovieResult(
    val continuitySummary: String,
    val scenes: List<MovieScene>
)

data class CharacterVisionExtraction(
    val name: String,
    val archetype: String,
    val visualDetails: String,
    val costumeDetails: String,
    val auraAndPalette: String,
    val continuityTags: List<String>
)

class BhaktiAiService {

    private val scenePlanSchema = Schema.obj(
        properties = mapOf(
            "continuitySummary" to Schema.string(
                description = "Cohesive visual continuity bible covering palette, lighting, and character consistency across all scenes."
            ),
            "scenes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "sceneNumber" to Schema.integer(description = "Sequential 1-based scene number"),
                        "title" to Schema.string(description = "Short evocative cinematic scene title"),
                        "durationSeconds" to Schema.integer(description = "Recommended scene duration between 4 and 60 seconds"),
                        "sceneDescription" to Schema.string(description = "Detailed mythological visual setting, mood, and divine lighting"),
                        "cameraDirection" to Schema.string(description = "Camera framing, lens choice, and motion direction"),
                        "characterActions" to Schema.string(description = "Character blocking, gestures, mudras, and costume highlights"),
                        "continuityNotes" to Schema.string(description = "Explicit continuity link carrying lighting, props, and appearance from the prior scene"),
                        "narrationScript" to Schema.string(description = "Devotional narration voice-over script in the requested language"),
                        "sanskritPhonetics" to Schema.string(description = "Optional IAST transliteration or Sanskrit pronunciation cue"),
                        "subtitleText" to Schema.string(description = "Clean on-screen subtitle text for this scene")
                    ),
                    optionalProperties = listOf("sanskritPhonetics")
                ),
                description = "Ordered list of 3 to 8 cinematic scenes."
            )
        )
    )

    private val characterVisionSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Suggested mythological character name or title"),
            "archetype" to Schema.string(description = "Role or divine archetype"),
            "visualDetails" to Schema.string(description = "Facial features, expression, posture, skin tone, and aura"),
            "costumeDetails" to Schema.string(description = "Crown, garments, jewelry, garlands, and sacred attributes"),
            "auraAndPalette" to Schema.string(description = "Dominant color palette and lighting mood"),
            "continuityTags" to Schema.array(
                items = Schema.string(),
                description = "Up to 6 short visual continuity keywords"
            )
        )
    )

    private val sanskritCoachSchema = Schema.obj(
        properties = mapOf(
            "devanagari" to Schema.string(description = "Refined devotional text or Devanagari shloka"),
            "iastTransliteration" to Schema.string(description = "Precise IAST Romanized transliteration"),
            "syllableBreakdown" to Schema.string(description = "Syllable-by-syllable pronunciation and stress guide"),
            "deliveryNotes" to Schema.string(description = "Voice acting cadence, pauses, and devotional tone cues")
        )
    )

    private val sceneDirectorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scenePlanSchema
                temperature = 0.7f
            }
        )
    }

    private val visionCharacterModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = characterVisionSchema
                temperature = 0.4f
            }
        )
    }

    private val voiceCoachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sanskritCoachSchema
                temperature = 0.4f
            }
        )
    }

    private val shotPacketModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.5f
            }
        )
    }

    suspend fun planMovieScenes(
        title: String,
        storyInput: String,
        language: String,
        visualStyle: String,
        characters: List<MythCharacter>
    ): PlannedMovieResult {
        val characterBible = if (characters.isEmpty()) {
            "No custom Character Library references attached; infer reverent traditional iconography."
        } else {
            characters.joinToString("\n") { c ->
                "- ${c.name} (${c.archetype}): Visuals: ${c.visualDetails}; Costume: ${c.costumeDetails}; Palette: ${c.auraAndPalette}; Tags: ${c.continuityTags.joinToString(", ")}"
            }
        }

        val prompt = """
            You are an acclaimed Indian mythological and devotional 3D CGI cinema director (Unreal Engine 5 / IMAX photorealistic VFX style).
            Analyze the user's story/script and divide it into 3 to 6 sequential cinematic 3D animated scenes.
            Preserve strict visual, costume, prop, 3D lighting, and spatial continuity from scene to scene.

            For every scene:
            - Describe photorealistic 3D depth, physically based materials (woven silk, hammered gold filigree, carved stone, luminous water & fire physics), volumetric god rays, and atmospheric particles (incense mist, embers, petals).
            - Specify exact 3D camera choreography & cinema lenses in cameraDirection (e.g., '35mm anamorphic orbital 3D arc, f/1.8 shallow depth of field', '24mm aerial crane sweep', '50mm slow dolly push-in').

            Project Title: $title
            Visual Style: $visualStyle (emphasize realistic 3D depth, global illumination, and filmic realism)
            Narration Language: $language (For 'hindi' write devotional Hindi in Devanagari; for 'sanskrit' write refined Sanskrit shloka/prose with IAST phonetics in sanskritPhonetics; for 'bilingual' write Hindi/Sanskrit narration with clear English subtitleText; for 'english' write elevated mythological English).

            Character Continuity Bible:
            $characterBible

            Story / Script:
            $storyInput
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                sceneDirectorModel.generateContent(prompt)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned an empty scene plan.")

            val root = JSONObject(rawJson)
            val continuitySummary = root.optString("continuitySummary", "")
            val scenesArr = root.getJSONArray("scenes")
            val scenes = (0 until scenesArr.length()).map { idx ->
                val item = scenesArr.getJSONObject(idx)
                MovieScene(
                    sceneNumber = item.optLong("sceneNumber", (idx + 1).toLong()),
                    title = item.optString("title", "Scene ${idx + 1}"),
                    durationSeconds = item.optLong("durationSeconds", 8L).coerceIn(4L, 60L),
                    sceneDescription = item.optString("sceneDescription", ""),
                    cameraDirection = item.optString("cameraDirection", ""),
                    characterActions = item.optString("characterActions", ""),
                    continuityNotes = item.optString("continuityNotes", ""),
                    narrationScript = item.optString("narrationScript", ""),
                    sanskritPhonetics = item.optString("sanskritPhonetics", ""),
                    subtitleText = item.optString("subtitleText", ""),
                    includedInCut = true
                )
            }
            if (scenes.isEmpty()) {
                throw IllegalStateException("No scenes were generated from the script.")
            }
            return PlannedMovieResult(
                continuitySummary = continuitySummary,
                scenes = scenes
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Scene planning timed out", e)
            throw IllegalStateException("Scene planning timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Scene planning failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate scene plan.", e)
        }
    }

    suspend fun extractCharacterFromImage(bitmap: Bitmap): CharacterVisionExtraction {
        try {
            val response = withTimeout(60_000L) {
                visionCharacterModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Analyze this mythological or devotional character reference artwork. " +
                                "Extract consistent visual details, attire, ornaments, color aura, and continuity tags for animated film production."
                        )
                    }
                )
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty vision response.")
            val json = JSONObject(rawJson)
            val tagsJson = json.optJSONArray("continuityTags")
            val tags = if (tagsJson != null) {
                (0 until tagsJson.length()).mapNotNull { tagsJson.optString(it).takeIf { s -> s.isNotBlank() } }
            } else emptyList()

            return CharacterVisionExtraction(
                name = json.optString("name", "Mythological Character"),
                archetype = json.optString("archetype", "Divine Archetype"),
                visualDetails = json.optString("visualDetails", ""),
                costumeDetails = json.optString("costumeDetails", ""),
                auraAndPalette = json.optString("auraAndPalette", ""),
                continuityTags = tags
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Character vision extraction timed out", e)
            throw IllegalStateException("Image analysis timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Character vision extraction failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze reference image.", e)
        }
    }

    suspend fun coachSanskritAndVoice(
        scriptText: String,
        targetLanguage: String
    ): SanskritCoachResult {
        val prompt = """
            You are a Vedic Sanskrit & Hindi devotional voice director.
            Prepare accurate pronunciation, Devanagari polish, IAST transliteration, syllable breakdown, and voice delivery cues for:
            Language: $targetLanguage
            Text:
            $scriptText
        """.trimIndent()

        try {
            val response = withTimeout(45_000L) {
                voiceCoachModel.generateContent(prompt)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response from Voice Coach.")
            val json = JSONObject(rawJson)
            return SanskritCoachResult(
                devanagari = json.optString("devanagari", scriptText),
                iastTransliteration = json.optString("iastTransliteration", ""),
                syllableBreakdown = json.optString("syllableBreakdown", ""),
                deliveryNotes = json.optString("deliveryNotes", "")
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Voice coaching timed out after 45s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Voice coaching failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze narration.", e)
        }
    }

    suspend fun optimizeVideoPrompt(
        basePrompt: String,
        mode: String,
        aspectRatio: String,
        durationSeconds: Long,
        characters: List<MythCharacter>,
        renderStyle3D: String = "Unreal 5 Photorealistic 3D",
        cameraRig3D: String = "Orbital 3D Arc (35mm)",
        lightingMood3D: String = "Volumetric God Rays"
    ): String {
        val charInfo = characters.joinToString("; ") {
            "${it.name}: ${it.costumeDetails}, ${it.auraAndPalette}"
        }
        val prompt = """
            Refine this devotional animated video shot prompt into a hyper-realistic 3D CGI cinema prompt (under 200 words).
            Include physically based 3D textures (silk weave, carved stone, hammered gold, water/fire physics), subsurface skin scattering, ray-traced reflections, f/1.8 anamorphic bokeh, and smooth 24fps filmic motion blur.
            Mode: $mode | Aspect Ratio: $aspectRatio | Duration: ${durationSeconds}s
            3D Render Engine: $renderStyle3D
            3D Camera Rig & Lens: $cameraRig3D
            Lighting & Atmosphere: $lightingMood3D
            Character Continuity: ${charInfo.ifBlank { "None attached" }}
            Draft Prompt: $basePrompt
        """.trimIndent()

        return try {
            val response = withTimeout(40_000L) {
                shotPacketModel.generateContent(prompt)
            }
            response.text?.trim()?.takeIf { it.isNotBlank() } ?: basePrompt
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prompt optimization failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to refine shot prompt.", e)
        }
    }

    private companion object {
        const val TAG = "BhaktiAiService"
    }
}
