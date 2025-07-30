/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service;

import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	private static final String TAG = "AlarmPingSender";

	// TODO: Add log.
	private ClientComms comms;
	private MqttService service;
	private AlarmPingSender that;
	private static final ConcurrentHashMap<String, ClientComms> clientCommsMap=new ConcurrentHashMap<>();
	private volatile boolean hasStarted = false;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.service = service;
		that = this;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		String key=comms.getClient().getClientId();
		clientCommsMap.put(key, comms);
	}

	@Override
	public void start() {
		String key=comms.getClient().getClientId();
		if (clientCommsMap.containsKey(key)){
			ClientComms clientComms = clientCommsMap.get(key);
			if (clientComms!=null&&clientComms.getClientState()!=null){
				schedule(clientComms.getKeepAlive());
			}
		}
		hasStarted = true;
	}

	@Override
	public void stop() {

		Log.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
		if(hasStarted){
				String key=comms.getClient().getClientId();
				WorkManager.getInstance(that.service).cancelAllWorkByTag("AlarmWorkerTAG"+key);

			hasStarted = false;
		}
	}
	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
		Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);

		OneTimeWorkRequest.Builder alarmWorkerBuilder = new OneTimeWorkRequest.Builder(AlarmWorker.class);
		Data.Builder data = new Data.Builder();
		String key=comms.getClient().getClientId();
		data.putString("key", key);

		alarmWorkerBuilder.setInitialDelay(delayInMilliseconds, TimeUnit.MILLISECONDS)
				.setInputData(data.build())
				.addTag("AlarmWorkerTAG"+key)
				.setConstraints(new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.setRequiresBatteryNotLow(true).build());
		OneTimeWorkRequest workRequest=alarmWorkerBuilder.build();
		String uniqueWorkName = "AlarmWorker"+key+System.currentTimeMillis();

		WorkManager.getInstance(that.service).enqueueUniqueWork(
				uniqueWorkName,
				ExistingWorkPolicy.REPLACE,
				workRequest
		);

	}
	public static ConcurrentHashMap<String, ClientComms> getClientCommsMap(){
		return clientCommsMap;
	}
}
