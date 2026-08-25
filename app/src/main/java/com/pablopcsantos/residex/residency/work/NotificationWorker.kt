package com.pablopcsantos.residex.residency.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pablopcsantos.residex.residency.data.toDomain
import com.pablopcsantos.residex.residency.data.local.SelectionDao
import com.pablopcsantos.residex.residency.data.local.SelectionPreferences
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import com.pablopcsantos.residex.residency.notification.NotificationDispatcher
import com.pablopcsantos.residex.residency.notification.NotificationDeduplication
import com.pablopcsantos.residex.residency.notification.NotificationHistory
import com.pablopcsantos.residex.residency.notification.NotificationPreferences
import com.pablopcsantos.residex.residency.notification.NotificationRules
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SelectionRepository,
    private val dao: SelectionDao,
    private val selectionPreferences: SelectionPreferences,
    private val notificationPreferences: NotificationPreferences,
    private val history: NotificationHistory,
    private val dispatcher: NotificationDispatcher
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = notificationPreferences.state.value
        if (!settings.enabled) return Result.success()
        repository.refresh()
        val selections = dao.getAll().filter { it.active }.map { it.toDomain() }
        val today = LocalDate.now()
        val candidates = NotificationRules.eligible(
            selections,
            selectionPreferences.state.value.followedIds.toSet(),
            settings,
            today
        )
        val sentKeys = candidates
            .map { NotificationDeduplication.key(it, settings.repeat, today) }
            .filter { history.wasSent(it) }
            .toSet()
        NotificationDeduplication.unsent(
            candidates,
            sentKeys,
            settings.repeat,
            today
        ).forEach { candidate ->
            val key = NotificationDeduplication.key(candidate, settings.repeat, today)
            if (
                !history.wasSent(key) &&
                dispatcher.notify(candidate) ==
                com.pablopcsantos.residex.residency.notification.NotificationPostResult.POSTED
            ) {
                history.markSent(key)
            }
        }
        return Result.success()
    }
}