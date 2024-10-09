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

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

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
	private BroadcastReceiver alarmReceiver;
	private AlarmPingSender that;
	private PendingIntent pendingIntent;
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
		this.alarmReceiver = new AlarmReceiver();
	}

	@Override
	public void start() {
		String action = MqttServiceConstants.PING_SENDER
				+ comms.getClient().getClientId();
		Log.d(TAG, "Register alarmreceiver to MqttService"+ action);
		IntentFilter filter=new IntentFilter(action);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
			service.registerReceiver(alarmReceiver, filter,Context.RECEIVER_EXPORTED);
		}else {
			service.registerReceiver(alarmReceiver, filter);
		}
		//pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(
		//		action), PendingIntent.FLAG_UPDATE_CURRENT);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(
					action), PendingIntent.FLAG_IMMUTABLE);
		}else {
			pendingIntent = PendingIntent.getBroadcast(service, 0, new Intent(
					action), PendingIntent.FLAG_UPDATE_CURRENT);
		}

		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {

		Log.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
		if(hasStarted){
			if(pendingIntent != null){
				// Cancel Alarm.
				//AlarmManager alarmManager = (AlarmManager) service.getSystemService(Service.ALARM_SERVICE);
				//alarmManager.cancel(pendingIntent);
				WorkManager.getInstance(that.service).cancelAllWorkByTag("AlarmWorkerTAG");
			}

			hasStarted = false;
			try{
				service.unregisterReceiver(alarmReceiver);
			}catch(IllegalArgumentException e){
				//Ignore unregister errors.			
			}
		}
	}
	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis()
				+ delayInMilliseconds;
		Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
		Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
			String action = MqttServiceConstants.PING_SENDER
					+ comms.getClient().getClientId();
			Intent intent = new Intent(action);
			that.service.sendBroadcast(intent);

	}

	/*
	 * This class sends PingReq packet to MQTT broker
	 */
	class AlarmReceiver extends BroadcastReceiver {
		//private WakeLock wakelock;
		private final String wakeLockTag = MqttServiceConstants.PING_WAKELOCK
				+ that.comms.getClient().getClientId();

		@Override
        @SuppressLint("Wakelock")
		public void onReceive(Context context, Intent intent) {
			// According to the docs, "Alarm Manager holds a CPU wake lock as
			// long as the alarm receiver's onReceive() method is executing.
			// This guarantees that the phone will not sleep until you have
			// finished handling the broadcast.", but this class still get
			// a wake lock to wait for ping finished.

			Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis()+intent.getAction());
			OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AlarmWorker.class)
					.addTag("AlarmWorkerTAG")
					.build();
			WorkManager.getInstance(that.service).enqueue(workRequest);
			WorkManager.getInstance(that.service).getWorkInfoByIdLiveData(workRequest.getId()).observe(that.service, workInfo ->{
				if (workInfo != null && workInfo.getState().isFinished()){
					Log.d(TAG, "Ping finished. Release lock(" + wakeLockTag + ")");
					IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

						@Override
						public void onSuccess(IMqttToken asyncActionToken) {
							Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
									+ System.currentTimeMillis());
							WorkManager.getInstance(that.service).cancelWorkById(workRequest.getId());
						}

						@Override
						public void onFailure(IMqttToken asyncActionToken,
											  Throwable exception) {
							Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
									+ System.currentTimeMillis());
							WorkManager.getInstance(that.service).cancelWorkById(workRequest.getId());
						}
					});
				}
			});
		}
	}
}
