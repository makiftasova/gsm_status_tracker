package com.makiftasova.gsmtracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

class GSMListenerService : Service() {

    private val logTag = "GSMListenerService"
    private val logFile = "GSMListenerLog.txt"
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        Log.e(this.logTag, "some component wants to bind with the service")
        /* no binding provided */
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(this.logTag, "onStartCommand received with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.i(this.logTag, "using an intent with action $action")
            when (action) {
                Actions.START.name -> startGsmService()
                Actions.STOP.name -> stopGsmService()
                else -> Log.e(this.logTag, "unknown action $action")
            }
        } else {
            Log.e(this.logTag, "intent is null")
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(this.logTag, "service has been created")
        startForeground(1, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(this.logTag, "service has been destroyed")
        Toast.makeText(this, "GSM Listener Service Destroyed", Toast.LENGTH_SHORT).show()
    }

    private fun startGsmService() {
        if (this.isServiceStarted) {
            Log.i(this.logTag, "service already started")
            return
        }

        Log.i(this.logTag, "starting foreground service")
        Toast.makeText(this, "GSM Listener Service is Starting", Toast.LENGTH_SHORT).show()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GSMListenerService::lock").apply {
                acquire()
            }
        }

        setServiceState(this, ServiceStateTracker.STARTED)
        this.isServiceStarted = true

        GlobalScope.launch(Dispatchers.Default) {
            while (isServiceStarted) {
                launch(Dispatchers.Default) {
                    pollGsmStatus()
                }
                delay(5 * 1000)
            }
        }
    }

    private fun stopGsmService() {
        Log.i(this.logTag, "stopping foreground service")
        Toast.makeText(this, "GSM Listener Service is Stopping", Toast.LENGTH_SHORT).show()

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e(this.logTag, "service stopped without being started: ${e.message}")
        }
        this.isServiceStarted = false
        setServiceState(this, ServiceStateTracker.STOPPED)
    }

    private fun simStateToString(state: Int): String {
        return when (state) {
            TelephonyManager.SIM_STATE_ABSENT -> "SIM_STATE_ABSENT"
            TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "SIM_STATE_CARD_IO_ERROR"
            TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "SIM_STATE_CARD_RESTRICTED"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "SIM_STATE_NETWORK_LOCKED"
            TelephonyManager.SIM_STATE_NOT_READY -> "SIM_STATE_NOT_READY"
            TelephonyManager.SIM_STATE_PERM_DISABLED -> "SIM_STATE_PERM_DISABLED"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "SIM_STATE_PIN_REQUIRED"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "SIM_STATE_PUK_REQUIRED"
            TelephonyManager.SIM_STATE_READY -> "SIM_STATE_READY"
            TelephonyManager.SIM_STATE_UNKNOWN -> "SIM_STATE_UNKNOWN"
            else -> "SIM_STATE_UNKNOWN"
        }
    }

    private fun appendToFile(name: String, line: String): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "GSMListener"
        )
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(this.logTag, "could not create directory $dir")
                return false
            }
        }

        Log.i(this.logTag, "Appending line to log file: $line")
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val file = File(dir, name)
        file.appendText("$timestamp - $line\n")
        Log.i(this.logTag, "Appended line to log file: $line")

        return true
    }

    private fun pollGsmStatus() {
        val telephonyManager: TelephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        for (i: Int in 0..1) {
            val state = telephonyManager.getSimState(i)
            when (state) {
                TelephonyManager.SIM_STATE_ABSENT -> Log.e(logTag, "SIM Slot $i is ABSENT")
                else -> Log.i(this.logTag, "Sim slot $i  state: ${simStateToString(state)}")
            }
            if (!appendToFile(this.logFile, "SIM slot #$i: ${simStateToString(state)}")) {
                Log.e(this.logTag, "could not log to file ${this.logFile}")
            }

        }
        return
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "GsmListenerServiceNotification"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notificationChannelId,
            "GSMListenerService",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "GSM Listener Service Channel"
            it.enableLights(false)
            it.enableVibration(false)
            it
        }

        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java)
            .let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val builder: Notification.Builder = Notification.Builder(this, notificationChannelId)

        return builder
            .setContentTitle("GSM Listener")
            .setContentText("Polling for GSM status changes")
            .setContentIntent(pendingIntent)
            .build()
    }
}
