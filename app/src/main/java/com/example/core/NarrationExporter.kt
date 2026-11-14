package com.example.core

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class ExportedAudio(
    val file: File,
    val durationMs: Long
)

/**
 * Turns narration text into an audio FILE (the existing Voice Studio only plays speech through the speaker).
 *
 *  - exportOnDevice: free, uses the phone's own text-to-speech engine, writes a WAV. Needs a Hindi voice
 *    installed on the device (Settings > System > Language > Text-to-speech output).
 *  - exportCloud: optional, calls Google Cloud Text-to-Speech with an API key, writes an MP3. Paid per character.
 */
object NarrationExporter {

    private const val MAX_ON_DEVICE_CHARS = 3900
    const val DEFAULT_CLOUD_VOICE = "hi-IN-Chirp3-HD-Charon"

    fun languageTagFor(voiceLanguage: String): String =
        if (voiceLanguage == "english") "en-IN" else "hi-IN"

    suspend fun exportOnDevice(
        context: Context,
        text: String,
        languageTag: String,
        speechRate: Float,
        outFile: File
    ): Result<ExportedAudio> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Narration text is empty."))
        if (text.length > MAX_ON_DEVICE_CHARS) {
            return Result.failure(
                IllegalArgumentException("Narration is too long for one export (max $MAX_ON_DEVICE_CHARS characters). Export it scene by scene.")
            )
        }
        outFile.parentFile?.mkdirs()
        var tts: TextToSpeech? = null
        try {
            val ready = suspendCancellableCoroutine<Boolean> { cont ->
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS)
                }
                cont.invokeOnCancellation { tts?.shutdown() }
            }
            val engine = tts
            if (!ready || engine == null) {
                return Result.failure(IllegalStateException("The device text-to-speech engine could not start."))
            }
            val locale = Locale.forLanguageTag(languageTag)
            val langResult = engine.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                return Result.failure(
                    IllegalStateException("This device has no ${locale.displayLanguage} voice installed. Install one in the phone's text-to-speech settings, or use the cloud voice.")
                )
            }
            engine.setSpeechRate(speechRate.coerceIn(0.5f, 1.5f))

            val written = suspendCancellableCoroutine<Boolean> { cont ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (cont.isActive) cont.resume(false)
                    }
                })
                val rc = engine.synthesizeToFile(text, Bundle(), outFile, "bhakti_export")
                if (rc != TextToSpeech.SUCCESS && cont.isActive) cont.resume(false)
            }
            return if (!written || !outFile.exists() || outFile.length() == 0L) {
                Result.failure(IllegalStateException("The voice engine could not write the audio file."))
            } else {
                Result.success(ExportedAudio(outFile, durationOf(outFile)))
            }
        } finally {
            tts?.shutdown()
        }
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    suspend fun exportCloud(
        apiKey: String,
        text: String,
        languageCode: String,
        voiceName: String,
        speakingRate: Float,
        outFile: File
    ): Result<ExportedAudio> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "No Google Cloud text-to-speech key is configured." }
            require(text.isNotBlank()) { "Narration text is empty." }
            val body = JSONObject()
                .put("input", JSONObject().put("text", text))
                .put("voice", JSONObject().put("languageCode", languageCode).put("name", voiceName))
                .put(
                    "audioConfig",
                    JSONObject()
                        .put("audioEncoding", "MP3")
                        .put("speakingRate", speakingRate.toDouble().coerceIn(0.25, 2.0))
                )
            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize")
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Cloud voice request failed (${resp.code}): ${errorMessage(raw)}")
                val audio = JSONObject(raw).optString("audioContent")
                require(audio.isNotEmpty()) { "Cloud voice returned no audio." }
                outFile.parentFile?.mkdirs()
                outFile.writeBytes(Base64.decode(audio, Base64.DEFAULT))
            }
            ExportedAudio(outFile, durationOf(outFile))
        }
    }

    private fun errorMessage(raw: String): String = try {
        JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty().ifBlank { raw.take(200) }
    } catch (_: Exception) {
        raw.take(200)
    }

    private fun durationOf(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
