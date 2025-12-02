package cz.maj.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CallRecordService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        L.i("CallRecordService.onCreate()")
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("PHONE_NUMBER")
        val isOutgoing = intent?.getBooleanExtra("IS_OUTGOING", false) ?: false

        L.i("onStartCommand: phoneNumber=$phoneNumber, isOutgoing=$isOutgoing")

        val contactName = lookupContactName(phoneNumber)
        L.d("lookupContactName: number=$phoneNumber -> name=$contactName")

        outputFile = createOutputFile(phoneNumber, contactName, isOutgoing)
        L.i("Creating output file: path=${outputFile?.absolutePath}")

        try {
            startRecording(outputFile!!)
            L.i("Recording STARTED")
        } catch (t: Throwable) {
            L.e("Failed to start recording", t)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        L.i("CallRecordService.onDestroy() – stopping recording")
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "call_recording"
        val channelName = "Nahrávání hovorů"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nahrávání hovoru")
            .setContentText("Probíhá záznam hovoru")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        L.d("startForegroundNotification()")
        startForeground(1, notification)
    }

    private fun startRecording(file: File) {
        L.d("startRecording(): file=${file.absolutePath}")
        mediaRecorder = MediaRecorder().apply {
            try {
                L.d("Trying AudioSource.VOICE_CALL")
                setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
            } catch (e: Exception) {
                L.w("VOICE_CALL source failed, trying VOICE_COMMUNICATION", e)
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            }

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                L.e("MediaRecorder prepare/start failed", e)
                reset()
                release()
                throw e
            }
        }
    }

    private fun stopRecording() {
        L.d("stopRecording() called")
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                    L.i("Recording STOPPED successfully")
                } catch (e: Exception) {
                    L.e("Error while stopping MediaRecorder", e)
                } finally {
                    reset()
                    release()
                }
            }
        } catch (e: Exception) {
            L.e("stopRecording() outer exception", e)
        } finally {
            mediaRecorder = null
        }
    }

    private fun lookupContactName(number: String?): String? {
        if (number == null) {
            L.d("lookupContactName: number is null")
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    L.d("lookupContactName: found name=$name for number=$number")
                    return name
                }
            }
            L.d("lookupContactName: no contact found for number=$number")
            null
        } catch (e: Exception) {
            L.e("lookupContactName: query failed", e)
            null
        }
    }

    private fun createOutputFile(
        number: String?,
        name: String?,
        outgoing: Boolean
    ): File {
        val now = LocalDateTime.now()
        val dir = RecordingUtils.getRecordingsDir(this)

        val direction = if (outgoing) "OUT" else "IN"
        val safeNumber = number ?: "unknown"
        val safeName = name?.replace("\s+".toRegex(), "_") ?: ""
        val timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        val fileName = if (safeName.isNotEmpty()) {
            "${timestamp}_${direction}_${safeNumber}_$safeName.m4a"
        } else {
            "${timestamp}_${direction}_${safeNumber}.m4a"
        }

        val file = File(dir, fileName)
        L.d("createOutputFile(): $fileName in dir=${dir.absolutePath}")
        return file
    }
}
