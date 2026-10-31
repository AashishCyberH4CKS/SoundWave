package com.bishify.player.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

object RingtoneTrimmer {
    private const val TAG = "RingtoneTrimmer"

    suspend fun trimAudio(
        context: Context,
        sourcePath: String,
        outputFileName: String,
        startMs: Long,
        endMs: Long
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val tempFile = File(context.cacheDir, "temp_ringtone_${System.currentTimeMillis()}.m4a")

        try {
            extractor.setDataSource(sourcePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                return@withContext Result.failure(Exception("No audio track found in file"))
            }

            extractor.selectTrack(audioTrackIndex)
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            // Determine output format based on input or default to MPEG_4
            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endMs * 1000L) {
                    break
                }

                if (presentationTimeUs >= startMs * 1000L) {
                    bufferInfo.presentationTimeUs = presentationTimeUs - (startMs * 1000L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            // Save into Android MediaStore Ringtones collection
            val cleanName = if (outputFileName.endsWith(".m4a", true)) outputFileName else "$outputFileName.m4a"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, cleanName)
                put(MediaStore.Audio.Media.TITLE, outputFileName.removeSuffix(".m4a"))
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES + "/BISHIFY")
                }
            }

            val ringtoneUri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            context.contentResolver.openOutputStream(ringtoneUri)?.use { out ->
                FileInputStream(tempFile).use { inStream ->
                    inStream.copyTo(out)
                }
            }

            tempFile.delete()
            Result.success(ringtoneUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming audio: ${e.message}", e)
            try {
                muxer?.release()
                extractor.release()
                tempFile.delete()
            } catch (ignored: Exception) {}
            Result.failure(e)
        }
    }
}

