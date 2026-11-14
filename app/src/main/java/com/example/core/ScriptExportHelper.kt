package com.example.core

import android.content.Context
import android.content.Intent
import com.example.data.model.MovieProject
import com.example.data.model.MovieScene
import com.example.data.model.MythCharacter

object ScriptExportHelper {

    fun buildSrtSubtitles(scenes: List<MovieScene>): String {
        val included = scenes.filter { it.includedInCut }.sortedBy { it.sceneNumber }
        if (included.isEmpty()) return ""
        val sb = StringBuilder()
        var currentMs = 0L
        included.forEachIndexed { index, scene ->
            val durationMs = (scene.durationSeconds.coerceAtLeast(2L)) * 1000L
            val endMs = currentMs + durationMs
            sb.append("${index + 1}\n")
            sb.append("${formatSrtTime(currentMs)} --> ${formatSrtTime(endMs)}\n")
            sb.append("${scene.subtitleText.ifBlank { scene.narrationScript.ifBlank { scene.title } }}\n\n")
            currentMs = endMs
        }
        return sb.toString()
    }

    fun buildProductionScriptMarkdown(
        project: MovieProject,
        scenes: List<MovieScene>,
        characters: List<MythCharacter>
    ): String {
        val sb = StringBuilder()
        sb.append("# ${project.title}\n")
        sb.append("- **Language**: ${project.language.uppercase()}\n")
        sb.append("- **Visual Style**: ${project.visualStyle}\n")
        sb.append("- **Continuity Bible**: ${project.continuitySummary.ifBlank { "N/A" }}\n\n")

        if (characters.isNotEmpty()) {
            sb.append("## Character Continuity References\n")
            characters.forEach { c ->
                sb.append("- **${c.name}** (${c.archetype}): ${c.visualDetails}. Costume: ${c.costumeDetails}. Aura: ${c.auraAndPalette}.\n")
            }
            sb.append("\n")
        }

        sb.append("## Cinematic Scene Breakdown\n\n")
        scenes.filter { it.includedInCut }.sortedBy { it.sceneNumber }.forEach { scene ->
            sb.append("### Scene ${scene.sceneNumber}: ${scene.title} (${scene.durationSeconds}s)\n")
            sb.append("- **Visual**: ${scene.sceneDescription}\n")
            sb.append("- **Camera**: ${scene.cameraDirection}\n")
            sb.append("- **Character Actions**: ${scene.characterActions}\n")
            sb.append("- **Continuity**: ${scene.continuityNotes}\n")
            sb.append("- **Narration**: ${scene.narrationScript}\n")
            if (scene.sanskritPhonetics.isNotBlank()) {
                sb.append("- **Sanskrit Phonetics**: ${scene.sanskritPhonetics}\n")
            }
            sb.append("- **Subtitle**: ${scene.subtitleText}\n\n")
        }
        return sb.toString()
    }

    fun shareTextPackage(context: Context, subject: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export $subject"))
    }

    private fun formatSrtTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val millis = ms % 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
