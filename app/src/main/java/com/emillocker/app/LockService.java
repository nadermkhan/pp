package com.emillocker.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class LockService extends Service {
	
	private static final String CHANNEL_ID = "EMILockerService";
	private static final int NOTIFICATION_ID = 1001;
	
	private Handler handler;
	private Runnable lockChecker;
	private SharedPreferences prefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		
		createNotificationChannel();
		startForeground(NOTIFICATION_ID, createNotification());
		
		handler = new Handler();
		lockChecker = new Runnable() {
			@Override
			public void run() {
				checkAndLock();
				handler.postDelayed(this, 1000); // Check every second
			}
		};
		handler.post(lockChecker);
	}
	
	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			"EMI Locker Service",
			NotificationManager.IMPORTANCE_LOW
			);
			channel.setDescription("Keeps device locked until payment is complete");
			
			NotificationManager manager = getSystemService(NotificationManager.class);
			if (manager != null) {
				manager.createNotificationChannel(channel);
			}
		}
	}
	
	private Notification createNotification() {
		Intent notificationIntent = new Intent(this, LockActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(
		this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
		
		return new NotificationCompat.Builder(this, CHANNEL_ID)
		.setContentTitle("EMI Locker Active")
		.setContentText("Device is locked")
		.setSmallIcon(android.R.drawable.ic_lock_idle_lock)
		.setContentIntent(pendingIntent)
		.setOngoing(true)
		.build();
	}
	
	private void checkAndLock() {
		if (!prefs.getBoolean("isLocked", false)) {
			stopSelf();
			return;
		}
		
		String currentApp = getCurrentApp();
		if (currentApp != null && !currentApp.equals(getPackageName())) {
			// Another app is in foreground, bring lock screen to front
			Intent intent = new Intent(this, LockActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
			Intent.FLAG_ACTIVITY_CLEAR_TOP |
			Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
		}
	}
	
	private String getCurrentApp() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
			long time = System.currentTimeMillis();
			List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
			time - 1000 * 1000, time);
			
			if (appList != null && appList.size() > 0) {
				SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
				for (UsageStats usageStats : appList) {
					mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
				}
				if (!mySortedMap.isEmpty()) {
					return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
				}
			}
		}
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (handler != null && lockChecker != null) {
			handler.removeCallbacks(lockChecker);
		}
		
		// Restart service if device is still locked
		if (prefs.getBoolean("isLocked", false)) {
			Intent restartIntent = new Intent(this, LockService.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(restartIntent);
				} else {
				startService(restartIntent);
			}
		}
	}
	
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		// Restart service when task is removed
		if (prefs.getBoolean("isLocked", false)) {
			Intent restartIntent = new Intent(this, LockService.class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				startForegroundService(restartIntent);
				} else {
				startService(restartIntent);
			}
		}
	}
}