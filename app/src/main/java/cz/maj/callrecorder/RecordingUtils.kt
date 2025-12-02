package cz.maj.callrecorder

import android.content.Context
import java.io.File

object RecordingUtils {

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadRecordings(context: Context): List<Recording> {
        val dir = getRecordingsDir(context)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".m4a") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Recording(it) }
            ?: emptyList()
    }
}
