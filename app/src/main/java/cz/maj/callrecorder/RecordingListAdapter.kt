package cz.maj.callrecorder

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import cz.maj.callrecorder.databinding.ItemRecordingBinding
import java.io.File

class RecordingListAdapter(
    private val context: Context,
    private var items: MutableList<Recording>
) : RecyclerView.Adapter<RecordingListAdapter.VH>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlaying: File? = null

    inner class VH(val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecordingBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val rec = items[position]
        val b = holder.binding

        b.txtFileName.text = rec.file.name

        b.btnPlay.setOnClickListener {
            if (currentlyPlaying == rec.file) {
                stopPlayback()
            } else {
                play(rec.file)
            }
        }

        b.btnRename.setOnClickListener {
            showRenameDialog(rec, position)
        }

        b.btnDelete.setOnClickListener {
            if (rec.file.delete()) {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun play(file: File) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        currentlyPlaying = file
        L.i("Playing recording: ${file.name}")
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        if (currentlyPlaying != null) {
            L.i("Stopped playback: ${currentlyPlaying?.name}")
        }
        currentlyPlaying = null
    }

    private fun showRenameDialog(rec: Recording, position: Int) {
        val input = EditText(context).apply {
            setText(rec.file.nameWithoutExtension)
        }
        AlertDialog.Builder(context)
            .setTitle("Přejmenovat")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val newFile = File(rec.file.parentFile, "$newName.m4a")
                    if (rec.file.renameTo(newFile)) {
                        items[position] = rec.copy(file = newFile)
                        notifyItemChanged(position)
                        L.i("Renamed recording: ${rec.file.name} -> ${newFile.name}")
                    } else {
                        L.w("Failed to rename file: ${rec.file.name}")
                    }
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }
}
