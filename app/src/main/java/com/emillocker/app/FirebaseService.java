package com.emillocker.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseService extends FirebaseMessagingService {
	
	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);
		
		Map<String, String> data = remoteMessage.getData();
		if (data.containsKey("action")) {
			String action = data.get("action");
			
			if ("unlock".equals(action)) {
				unlockDevice();
				} else if ("lock".equals(action)) {
				lockDevice();
			}
		}
	}
	
	private void unlockDevice() {
		SharedPreferences prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("isLocked", false);
		editor.apply();
		
		// Stop lock service
		stopService(new Intent(this, LockService.class));
	}
	
	private void lockDevice() {
		SharedPreferences prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("isLocked", true);
		editor.apply();
		
		// Start lock service
		Intent serviceIntent = new Intent(this, LockService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
			} else {
			startService(serviceIntent);
		}
		
		// Start lock activity
		Intent lockIntent = new Intent(this, LockActivity.class);
		lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(lockIntent);
	}
	
	@Override
	public void onNewToken(String token) {
		super.onNewToken(token);
		
		// Save token to SharedPreferences
		SharedPreferences prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("fcmToken", token);
		editor.apply();
		
		// Send token to server
		String imei1 = prefs.getString("imei1", "");
		if (!imei1.isEmpty()) {
			ApiHelper.updateFCMToken(imei1, token, new ApiHelper.ApiCallback() {
				@Override
				public void onSuccess(String response) {
					// Token updated
				}
				
				@Override
				public void onError(String error) {
					// Error updating token
				}
			});
		}
	}
}