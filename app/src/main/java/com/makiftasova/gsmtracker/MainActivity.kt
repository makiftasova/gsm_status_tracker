package com.makiftasova.gsmtracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val logTag = "GSMListenerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_start).let {
            it.setOnClickListener {
                Log.i(this.logTag, "starting foreground service")
                actionOnService(Actions.START)
            }
        }

        findViewById<Button>(R.id.button_stop).let {
            it.setOnClickListener {
                Log.i(this.logTag, "stopping foreground service")
                actionOnService(Actions.STOP)
            }
        }

        val textStatus = findViewById<TextView>(R.id.text_status)
        textStatus.setText(R.string.status_unknown)


        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                when (getServiceState(applicationContext)) {
                    ServiceStateTracker.STARTED -> textStatus.setText(R.string.status_running)
                    ServiceStateTracker.STOPPED -> textStatus.setText(R.string.status_stopped)
                    else -> textStatus.setText(R.string.status_unknown)
                }
                mainHandler.postDelayed(this, 1000)
            }
        })


    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceStateTracker.STOPPED && action == Actions.STOP)
            return

        if (getServiceState(this) == ServiceStateTracker.STARTED && action == Actions.START)
            return


        Intent(this, GSMListenerService::class.java).also {
            it.action = action.name
            Log.i(logTag, "starting GSMListenerService")
            startForegroundService(it)
        }
    }
}