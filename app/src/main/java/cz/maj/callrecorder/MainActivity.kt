package cz.maj.callrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnStartStop: Button
    private lateinit var btnPlayLast: Button
    private lateinit var tvStatus: TextView

    private var adapter: RecordingListAdapter? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    private var currentLocation: Location? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val permissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        L.d("Permissions result: $result")
        if (hasAllPermissions()) {
            L.i("All required permissions granted")
            loadList()
            updateLocation()
        } else {
            L.w("Not all permissions granted – recording may not work correctly")
            loadList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L.i("MainActivity.onCreate()")
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recordingsRecycler)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnPlayLast = findViewById(R.id.btnPlayLast)
        tvStatus = findViewById(R.id.tvStatus)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnStartStop.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        btnPlayLast.setOnClickListener {
            playLastRecording()
        }

        if (!hasAllPermissions()) {
            L.i("Requesting permissions...")
            requestPerms.launch(permissions)
        } else {
            L.i("Permissions already granted")
            loadList()
            updateLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun hasAllPermissions(): Boolean {
        val ok = permissions.all {
            val granted = ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            L.d("Permission $it granted=$granted")
            granted
        }
        return ok
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
                L.d("Location updated: ${location?.latitude}, ${location?.longitude}")
            }
        }
    }

    private fun startRecording() {
        if (!hasAllPermissions()) {
            tvStatus.text = "Chybí oprávnění!"
            return
        }

        updateLocation()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timestamp.m4a"
        currentRecordingFile = File(getExternalFilesDir(null), fileName)

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentRecordingFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            btnStartStop.text = "STOP NAHRÁVÁNÍ"
            tvStatus.text = "Nahrávám... ${currentRecordingFile?.name}"
            L.i("Recording started: ${currentRecordingFile?.absolutePath}")

        } catch (e: Exception) {
            L.e("Failed to start recording", e)
            tvStatus.text = "Chyba nahrávání: ${e.message}"
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            btnStartStop.text = "START NAHRÁVÁNÍ"

            val recording = Recording(
                file = currentRecordingFile!!,
                phoneNumber = null,
                timestamp = Date(),
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
            )

            tvStatus.text = "Uloženo: ${recording.file.name}\nLokace: ${recording.getLocationString()}"
            L.i("Recording stopped and saved: ${recording.file.absolutePath}")

            loadList()

        } catch (e: Exception) {
            L.e("Failed to stop recording", e)
            tvStatus.text = "Chyba při ukládání: ${e.message}"
        }
    }

    private fun playLastRecording() {
        val recordings = RecordingUtils.loadRecordings(this)
        if (recordings.isEmpty()) {
            tvStatus.text = "Žádné nahrávky"
            return
        }

        val lastRecording = recordings.first()

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(lastRecording.file.absolutePath)
                prepare()
                setOnCompletionListener {
                    tvStatus.text = "Přehrávání dokončeno"
                }
                start()
            }

            tvStatus.text = "Přehrávám: ${lastRecording.file.name}"
            L.i("Playing: ${lastRecording.file.absolutePath}")

        } catch (e: Exception) {
            L.e("Failed to play recording", e)
            tvStatus.text = "Chyba přehrávání: ${e.message}"
        }
    }

    private fun loadList() {
        val recordings = RecordingUtils.loadRecordings(this).toMutableList()
        L.i("Loaded ${recordings.size} recordings")
        adapter = RecordingListAdapter(this, recordings)
        recycler.adapter = adapter
    }
}
