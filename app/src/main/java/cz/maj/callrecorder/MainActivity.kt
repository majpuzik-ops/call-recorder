package cz.maj.callrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MainActivity : ComponentActivity() {

    private lateinit var recycler: RecyclerView
    private var adapter: RecordingListAdapter? = null

    private val permissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS
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

        if (!hasAllPermissions()) {
            L.i("Requesting permissions...")
            requestPerms.launch(permissions)
        } else {
            L.i("Permissions already granted")
            loadList()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val ok = permissions.all {
            val granted = ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            L.d("Permission $it granted=$granted")
            granted
        }
        return ok
    }

    private fun loadList() {
        val recordings = RecordingUtils.loadRecordings(this).toMutableList()
        L.i("Loaded ${recordings.size} recordings")
        adapter = RecordingListAdapter(this, recordings)
        recycler.adapter = adapter
    }
}
