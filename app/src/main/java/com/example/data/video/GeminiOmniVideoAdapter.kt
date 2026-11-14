package com.example.data.video

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Real video rendering through the Gemini API (Interactions endpoint, model "gemini-omni-1.1-flash").
 * Text-to-video, or image-to-video when a reference image is supplied. The call is synchronous: the MP4 comes
 * back base64-encoded inside the response, which this adapter decodes and writes to [outFile].
 *
 * This is a PAID API. It is only called when the person presses the render button.
 * Model name and request shape follow Google's current docs (https://ai.google.dev/gemini-api/docs/omni);
 * Google has retired earlier video models before, so re-check the docs if requests start failing.
 */
class GeminiOmniVideoAdapter(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {

    companion object {
        const val DEFAULT_MODEL = "gemini-omni-1.1-flash"
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"
        private const val MAX_REFERENCE_BYTES = 12L * 1024 * 1024
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .callTimeout(12, TimeUnit.MINUTES)
            .build()
    }

    suspend fun generate(
        prompt: String,
        aspectRatio: String,
        resolution: String,
        referenceImage: File?,
        outFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "No Gemini API key is configured." }
            require(prompt.isNotBlank()) { "Shot prompt is empty." }

            val input: Any = if (referenceImage != null && referenceImage.exists()) {
                require(referenceImage.length() <= MAX_REFERENCE_BYTES) { "Reference image is too large (max 12 MB)." }
                val mime = when (referenceImage.extension.lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                JSONArray()
                    .put(
                        JSONObject()
                            .put("type", "image")
                            .put("data", Base64.encodeToString(referenceImage.readBytes(), Base64.NO_WRAP))
                            .put("mime_type", mime)
                    )
                    .put(JSONObject().put("type", "text").put("text", prompt))
            } else {
                prompt
            }

            val body = JSONObject()
                .put("model", model)
                .put("input", input)
                .put(
                    "response_format",
                    JSONObject()
                        .put("type", "video")
                        .put("aspect_ratio", aspectRatio)
                        .put("resolution", resolution)
                )
                .put("background", false)
                .put("store", false)
                .put("stream", false)

            val request = Request.Builder()
                .url(ENDPOINT)
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Video request failed (${resp.code}): ${errorMessage(raw)}")
                val json = JSONObject(raw)
                if (json.has("error")) error("Video request failed: ${errorMessage(raw)}")
                val b64 = findVideoData(json)
                    ?: error("The response contained no video. Status: ${json.optString("status", "unknown")}.")
                outFile.parentFile?.mkdirs()
                outFile.writeBytes(Base64.decode(b64, Base64.DEFAULT))
            }
            outFile
        }
    }

    /** Walks the response for the first content item of type "video" and returns its base64 data. */
    private fun findVideoData(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                if (node.optString("type") == "video" && node.optString("data").isNotEmpty()) {
                    return node.getString("data")
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    findVideoData(node.opt(keys.next()))?.let { return it }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findVideoData(node.opt(i))?.let { return it }
                }
            }
        }
        return null
    }

    private fun errorMessage(raw: String): String = try {
        val err = JSONObject(raw).opt("error")
        when (err) {
            is JSONObject -> err.optString("message").ifBlank { raw.take(200) }
            is String -> err
            else -> raw.take(200)
        }
    } catch (_: Exception) {
        raw.take(200)
    }
}
