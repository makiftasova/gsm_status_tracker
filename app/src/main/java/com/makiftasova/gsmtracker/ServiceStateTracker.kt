package com.makiftasova.gsmtracker

import android.content.Context
import android.content.SharedPreferences

enum class ServiceStateTracker {
    STARTED,
    STOPPED
}

private const val name = "GSMServiceStateTrackerKey"
private const val key = "GSMServiceStateTrackerState"

fun setServiceState(context: Context, state: ServiceStateTracker) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceStateTracker? {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceStateTracker.STOPPED.name)
    return value?.let { ServiceStateTracker.valueOf(it) }
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}