package org.eclipse.paho.android.service

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.withTimeout

/**
 * @date: 2024-10-08 17:19
 * @author: mayz
 * @version: 1.0
 */
class NetworkConnectionWorker(val context: Context,workerParams: WorkerParameters):
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withTimeout(10*60*1000){
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
             if (network != null) {
                Result.success()
            }else{
                Result.retry()
            }
            return@withTimeout Result.success()
        }
    }
}