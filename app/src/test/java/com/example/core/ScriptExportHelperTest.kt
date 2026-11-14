package com.example.core

import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptExportHelperTest {

    @Test
    fun buildSrtSubtitles_formatsValidTimedCues() {
        val scenes = listOf(
            MovieScene(
                sceneNumber = 1,
                title = "Dawn over Kshira Sagara",
                durationSeconds = 8,
                subtitleText = "At twilight, the gods gather by the Milk Ocean.",
                includedInCut = true
            ),
            MovieScene(
                sceneNumber = 2,
                title = "Hidden Scene",
                durationSeconds = 5,
                subtitleText = "Should be excluded",
                includedInCut = false
            ),
            MovieScene(
                sceneNumber = 3,
                title = "Appearance of Kurma",
                durationSeconds = 6,
                subtitleText = "Lord Vishnu steadies Mount Mandara.",
                includedInCut = true
            )
        )

        val srt = ScriptExportHelper.buildSrtSubtitles(scenes)
        assertTrue(srt.contains("00:00:00,000 --> 00:00:08,000"))
        assertTrue(srt.contains("00:00:08,000 --> 00:00:14,000"))
        assertTrue(!srt.contains("Should be excluded"))
    }

    @Test
    fun buildProductionScriptMarkdown_includesContinuityAndCharacters() {
        val project = MovieProject(
            title = "Samudra Manthan",
            language = "hindi",
            visualStyle = "Epic 3D Mythological",
            continuitySummary = "Warm saffron twilight throughout."
        )
        val scenes = listOf(
            MovieScene(
                sceneNumber = 1,
                title = "The Gathering",
                durationSeconds = 8,
                sceneDescription = "Devas stand in golden armor.",
                cameraDirection = "Slow crane push-in",
                narrationScript = "दिव्य सागर के तट पर देवता एकत्रित हुए।",
                subtitleText = "The Devas gathered on the celestial shore.",
                includedInCut = true
            )
        )
        val characters = listOf(
            MythCharacter(
                name = "Shri Vishnu",
                archetype = "Preserver",
                visualDetails = "Serene lotus eyes",
                costumeDetails = "Pitambara silk",
                auraAndPalette = "Antique gold"
            )
        )

        val md = ScriptExportHelper.buildProductionScriptMarkdown(project, scenes, characters)
        assertTrue(md.contains("# Samudra Manthan"))
        assertTrue(md.contains("Shri Vishnu"))
        assertTrue(md.contains("Slow crane push-in"))
        assertTrue(md.contains("Warm saffron twilight throughout."))
    }
}
