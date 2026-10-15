package com.example.report

import android.content.Context
import android.net.Uri
import java.io.File

object LocalMediaStore {
    fun import(context: Context, source: Uri, prefix: String): String {
        val dir = File(context.filesDir, "private_media").apply { mkdirs() }
        val target = File.createTempFile(prefix.take(20), ".jpg", dir)
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Selected file could not be opened." }
            target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
        require(target.length() in 1..15_000_000) { target.delete(); "Image must be smaller than 15 MB." }
        return Uri.fromFile(target).toString()
    }
}
