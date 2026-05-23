package com.xesc.asltv.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.xesc.asltv.data.repository.ChannelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ListSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ChannelRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Refresca todas las listas guardadas (solo si han pasado 6h)
            val lists = repository.getAllSavedLists().first()
            lists.forEach { list ->
                repository.refreshList(list.userUrl, force = false)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ListSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "list_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}