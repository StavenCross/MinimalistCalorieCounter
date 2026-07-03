package com.makstuff.minimalistcaloriecounter.io

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi

internal class DownloadsTextWriter(private val context: Context) {
    fun write(filename: String, mimeType: String, content: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeWithMediaStore(filename, mimeType, content)
        } else {
            writeLegacy(filename, content)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeWithMediaStore(filename: String, mimeType: String, content: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create Downloads export file.")
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Could not write Downloads export file.")
        return "Downloads/$filename"
    }

    private fun writeLegacy(filename: String, content: String): String {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val file = java.io.File(dir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file.absolutePath
    }
}
