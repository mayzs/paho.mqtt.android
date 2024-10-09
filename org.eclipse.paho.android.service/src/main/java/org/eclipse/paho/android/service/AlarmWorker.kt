package org.eclipse.paho.android.service

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay


/**
 * @date: 2024-10-08 17:19
 * @author: mayz
 * @version: 1.0
 */
class AlarmWorker(val context: Context, workerParams: WorkerParameters):
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        delay(500L)
        return Result.success()
    }
}