package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

data class CopiedAudioInfo(
    val fileName: String,
    val localPath: String,
    val durationMs: Long
)

object MediaStorageHelper {

    suspend fun copyImageToAppStorage(
        context: Context,
        uri: Uri,
        subfolder: String = "images"
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "bhakti_media/$subfolder").apply { mkdirs() }
        val outFile = File(dir, "img_${UUID.randomUUID().toString().take(12)}.jpg")

        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Could not decode selected image.")

        val maxDim = 1024
        val scale = if (max(rawBitmap.width, rawBitmap.height) > maxDim) {
            maxDim.toFloat() / max(rawBitmap.width, rawBitmap.height).toFloat()
        } else {
            1f
        }
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                rawBitmap,
                (rawBitmap.width * scale).toInt().coerceAtLeast(1),
                (rawBitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            rawBitmap
        }

        FileOutputStream(outFile).use { fos ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        if (finalBitmap !== rawBitmap) {
            finalBitmap.recycle()
        }
        rawBitmap.recycle()
        outFile.absolutePath
    }

    suspend fun decodeBitmapForAi(
        context: Context,
        localPath: String,
        maxDimension: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(localPath)
        if (!file.exists()) return@withContext null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var inSample = 1
        while (opts.outWidth / inSample > maxDimension || opts.outHeight / inSample > maxDimension) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
        BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }

    suspend fun copyAudioToAppStorage(
        context: Context,
        uri: Uri
    ): CopiedAudioInfo = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "bhakti_media/audio").apply { mkdirs() }
        val displayName = queryDisplayName(context, uri) ?: "narration_${System.currentTimeMillis()}.m4a"
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(dir, "${UUID.randomUUID().toString().take(8)}_$safeName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read selected audio file.")

        val durationMs = extractAudioDurationMs(outFile.absolutePath)
        CopiedAudioInfo(
            fileName = displayName,
            localPath = outFile.absolutePath,
            durationMs = durationMs
        )
    }

    private fun extractAudioDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
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

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }
}
