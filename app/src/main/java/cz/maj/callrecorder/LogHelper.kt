package cz.maj.callrecorder

import android.util.Log

object L {

    private const val TAG = "CallRecorder"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun i(msg: String) {
        Log.i(TAG, msg)
    }

    fun w(msg: String, t: Throwable? = null) {
        if (t != null) Log.w(TAG, msg, t) else Log.w(TAG, msg)
    }

    fun e(msg: String, t: Throwable? = null) {
        if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg)
    }
}
