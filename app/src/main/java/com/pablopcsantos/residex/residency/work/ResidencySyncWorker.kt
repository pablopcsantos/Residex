package com.pablopcsantos.residex.residency.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pablopcsantos.residex.residency.domain.repository.RefreshResult
import com.pablopcsantos.residex.residency.domain.repository.SelectionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ResidencySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SelectionRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (repository.refresh()) {
        is RefreshResult.Updated -> Result.success()
        is RefreshResult.Failed -> if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}