package com.emillocker.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LockActivity extends AppCompatActivity {
	
	private TextView tvMessage, tvDealerInfo;
	private SharedPreferences prefs;
	private DatabaseReference deviceRef;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Make activity full screen and prevent screenshots
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
		WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
		WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
		WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
		WindowManager.LayoutParams.FLAG_SECURE);
		
		setContentView(R.layout.activity_lock);
		
		prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		
		tvMessage = findViewById(R.id.tvMessage);
		tvDealerInfo = findViewById(R.id.tvDealerInfo);
		
		loadDealerInfo();
		checkLockStatus();
	}
	
	private void loadDealerInfo() {
		String dealerId = prefs.getString("dealerId", "");
		String customerName = prefs.getString("customerName", "");
		
		tvDealerInfo.setText("Dealer ID: " + dealerId + "\nCustomer: " + customerName);
	}
	
	private void checkLockStatus() {
		String imei1 = prefs.getString("imei1", "");
		String deviceKey = imei1.replaceAll("[^a-zA-Z0-9]", "");
		
		deviceRef = FirebaseDatabase.getInstance().getReference("devices").child(deviceKey);
		
		deviceRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					Boolean isLocked = dataSnapshot.child("isLocked").getValue(Boolean.class);
					if (isLocked != null && !isLocked) {
						// Device is unlocked
						unlockDevice();
					}
				}
			}
			
			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Handle error
			}
		});
	}
	
	private void unlockDevice() {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("isLocked", false);
		editor.apply();
		
		// Stop lock service
		stopService(new android.content.Intent(this, LockService.class));
		
		// Allow user to exit
		finish();
	}
	
	@Override
	public void onBackPressed() {
		// Prevent back button
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Disable all keys
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Ensure activity stays on top
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Restart activity if still locked
		if (prefs.getBoolean("isLocked", false)) {
			android.content.Intent intent = new android.content.Intent(this, LockActivity.class);
			intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}
}