package cz.maj.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState: String? = null
        private var isOutgoingCall = false
        private var currentNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        L.d("onReceive: action=$action, extras=${intent.extras}")

        if (Intent.ACTION_NEW_OUTGOING_CALL == action) {
            isOutgoingCall = true
            currentNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            L.i("Outgoing call detected, number=$currentNumber")
            return
        }

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == action ||
            "android.intent.action.PHONE_STATE" == action) {

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            L.d("PHONE_STATE_CHANGED: state=$state, incoming=$incoming, lastState=$lastState")

            if (state == null || state == lastState) {
                L.d("State null or unchanged – ignoring")
                return
            }
            lastState = state

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    isOutgoingCall = false
                    currentNumber = incoming
                    L.i("Incoming call ringing, number=$currentNumber")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    L.i("Call OFFHOOK – starting recording service, number=$currentNumber, outgoing=$isOutgoingCall")
                    startRecordingService(context)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    L.i("Call IDLE – stopping recording service")
                    stopRecordingService(context)
                    currentNumber = null
                }
            }
        }
    }

    private fun startRecordingService(context: Context) {
        val serviceIntent = Intent(context, CallRecordService::class.java).apply {
            putExtra("PHONE_NUMBER", currentNumber)
            putExtra("IS_OUTGOING", isOutgoingCall)
        }
        L.d("startRecordingService: number=$currentNumber, outgoing=$isOutgoingCall")
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun stopRecordingService(context: Context) {
        val serviceIntent = Intent(context, CallRecordService::class.java)
        L.d("stopRecordingService() called")
        context.stopService(serviceIntent)
    }
}
