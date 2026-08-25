package com.pablopcsantos.residex.residency.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NotificationScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun schedule(frequency: com.pablopcsantos.residex.residency.notification.NotificationFrequency) {
        val now = LocalDateTime.now()
        val (period, first) = when (frequency) {
            com.pablopcsantos.residex.residency.notification.NotificationFrequency.DAILY -> 24L to nextAt(now, listOf(LocalTime.of(8, 0)))
            com.pablopcsantos.residex.residency.notification.NotificationFrequency.TWICE_DAILY -> 12L to nextAt(now, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))
            com.pablopcsantos.residex.residency.notification.NotificationFrequency.EVERY_SIX_HOURS -> 6L to now.plusMinutes(30)
        }
        val delay = Duration.between(now, first).toMinutes().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(period, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("residency-notifications", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() = WorkManager.getInstance(context).cancelUniqueWork("residency-notifications")

    private fun nextAt(now: LocalDateTime, times: List<LocalTime>): LocalDateTime = times
        .map { now.toLocalDate().atTime(it) }
        .map { if (it <= now) it.plusDays(1) else it }
        .minBy { it }
}