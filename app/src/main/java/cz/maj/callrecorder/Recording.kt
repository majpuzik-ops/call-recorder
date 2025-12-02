package cz.maj.callrecorder

import java.io.File
import java.util.Date

data class Recording(
    val file: File,
    val phoneNumber: String? = null,
    val timestamp: Date = Date(),
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun getLocationString(): String {
        return if (latitude != null && longitude != null) {
            String.format("%.6f, %.6f", latitude, longitude)
        } else {
            "Bez lokace"
        }
    }
}
