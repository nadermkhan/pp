package com.emillocker.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null) {
			SharedPreferences prefs = context.getSharedPreferences("EMILocker", Context.MODE_PRIVATE);
			
			if (prefs.getBoolean("isLocked", false)) {
				// Start lock service
				Intent serviceIntent = new Intent(context, LockService.class);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					context.startForegroundService(serviceIntent);
					} else {
					context.startService(serviceIntent);
				}
				
				// Start lock activity
				Intent lockIntent = new Intent(context, LockActivity.class);
				lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				context.startActivity(lockIntent);
			}
		}
	}
}