package com.emillocker.app;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class AdminReceiver extends DeviceAdminReceiver {
	
	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
		Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDisabled(Context context, Intent intent) {
		super.onDisabled(context, intent);
		Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public CharSequence onDisableRequested(Context context, Intent intent) {
		SharedPreferences prefs = context.getSharedPreferences("EMILocker", Context.MODE_PRIVATE);
		if (prefs.getBoolean("isLocked", false)) {
			return "Cannot disable admin while device is locked. Please contact your dealer.";
		}
		return "Are you sure you want to disable Device Admin?";
	}
	
	@Override
	public void onPasswordChanged(Context context, Intent intent) {
		super.onPasswordChanged(context, intent);
	}
	
	@Override
	public void onPasswordFailed(Context context, Intent intent) {
		super.onPasswordFailed(context, intent);
	}
	
	@Override
	public void onPasswordSucceeded(Context context, Intent intent) {
		super.onPasswordSucceeded(context, intent);
	}
}