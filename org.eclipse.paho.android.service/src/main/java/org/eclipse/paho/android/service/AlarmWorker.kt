package org.eclipse.paho.android.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import kotlin.coroutines.resume


/**
 * @date: 2024-10-08 17:19
 * @author: mayz
 * @version: 1.0
 */
class AlarmWorker(val context: Context, workerParams: WorkerParameters):
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        suspendCancellableCoroutine { continuation ->

            val key = this.inputData.getString("key")
            //check if id is not null
            if (key == null) {
                continuation.resume(Result.failure())
                return@suspendCancellableCoroutine
            }

            //check if there is a clients comm asociated with the key
            if (!AlarmPingSender.getClientCommsMap().containsKey(key)) {
                continuation.resume(Result.failure())
                return@suspendCancellableCoroutine
            }
            val wakeLockTag = (MqttServiceConstants.PING_WAKELOCK
                    + AlarmPingSender.getClientCommsMap()[key]?.client?.clientId)
            AlarmPingSender.getClientCommsMap()[key]?.checkForActivity(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("AlarmWorker", ("Success. Release lock(" + wakeLockTag + "):"
                                + System.currentTimeMillis())
                    )
                    continuation.resume(Result.success())
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("AlarmWorker", ("Failure. Release lock(" + wakeLockTag + "):"
                                + System.currentTimeMillis())
                    )
                    continuation.resume(Result.failure())
                }
            }) ?: kotlin.run {
                // when token is null doesn't always mean a failure sometimes there wasn't a need
                // send a ping request yet
                continuation.resume(Result.success())
            }
        }
}