package com.pablopcsantos.residex.residency.notification

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationHistory @Inject constructor(@ApplicationContext context: Context) {
    private val storage = context.getSharedPreferences("residency_notification_history", Context.MODE_PRIVATE)

    fun wasSent(key: String): Boolean = storage.getBoolean(key, false)
    fun markSent(key: String) { storage.edit { putBoolean(key, true) } }
    fun clear() { storage.edit { clear() } }
}