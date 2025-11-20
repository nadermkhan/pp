package com.emillocker.app;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
	
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	private static final int REQUEST_PERMISSION = 100;
	
	private EditText etDealerId, etCustomerName, etCustomerPhone;
	private TextView tvImei1, tvImei2, tvStatus;
	private Button btnRegister;
	
	private DevicePolicyManager devicePolicyManager;
	private ComponentName adminComponent;
	private SharedPreferences prefs;
	
	private String imei1 = "";
	private String imei2 = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		prefs = getSharedPreferences("EMILocker", MODE_PRIVATE);
		
		// Check if already registered and locked
		if (prefs.getBoolean("isLocked", false)) {
			startLockActivity();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_main);
		
		initViews();
		setupDeviceAdmin();
		requestPermissions();
		getIMEI();
		setupListeners();
	}
	
	private void initViews() {
		etDealerId = findViewById(R.id.etDealerId);
		etCustomerName = findViewById(R.id.etCustomerName);
		etCustomerPhone = findViewById(R.id.etCustomerPhone);
		tvImei1 = findViewById(R.id.tvImei1);
		tvImei2 = findViewById(R.id.tvImei2);
		tvStatus = findViewById(R.id.tvStatus);
		btnRegister = findViewById(R.id.btnRegister);
	}
	
	private void setupDeviceAdmin() {
		devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		adminComponent = new ComponentName(this, AdminReceiver.class);
	}
	
	private void requestPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,
				new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PERMISSION);
			}
		}
	}
	
	private void getIMEI() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					imei1 = telephonyManager.getImei(0);
					imei2 = telephonyManager.getImei(1);
					} else {
					imei1 = telephonyManager.getDeviceId(0);
					imei2 = telephonyManager.getDeviceId(1);
				}
				} catch (Exception e) {
				imei1 = Build.SERIAL;
				imei2 = android.provider.Settings.Secure.getString(getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
			}
			
			if (imei1 == null || imei1.isEmpty()) {
				imei1 = android.provider.Settings.Secure.getString(getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
			}
			if (imei2 == null || imei2.isEmpty()) {
				imei2 = Build.SERIAL;
			}
			
			tvImei1.setText("IMEI 1: " + imei1);
			tvImei2.setText("IMEI 2: " + imei2);
		}
	}
	
	private void setupListeners() {
		btnRegister.setOnClickListener(v -> {
			String dealerId = etDealerId.getText().toString().trim();
			String customerName = etCustomerName.getText().toString().trim();
			String customerPhone = etCustomerPhone.getText().toString().trim();
			
			if (dealerId.isEmpty() || customerName.isEmpty() || customerPhone.isEmpty()) {
				Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (!devicePolicyManager.isAdminActive(adminComponent)) {
				enableDeviceAdmin();
				} else {
				registerDevice(dealerId, customerName, customerPhone);
			}
		});
	}
	
	private void enableDeviceAdmin() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
		"This app needs Device Administrator permission to prevent unauthorized factory reset and app uninstall.");
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	}
	
	private void registerDevice(String dealerId, String customerName, String customerPhone) {
		tvStatus.setText("Registering device...");
		btnRegister.setEnabled(false);
		
		// Save to SharedPreferences
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("dealerId", dealerId);
		editor.putString("customerName", customerName);
		editor.putString("customerPhone", customerPhone);
		editor.putString("imei1", imei1);
		editor.putString("imei2", imei2);
		editor.putBoolean("isRegistered", true);
		editor.putBoolean("isLocked", true);
		editor.apply();
		
		// Register to Firebase
		DatabaseReference database = FirebaseDatabase.getInstance().getReference("devices");
		String deviceKey = imei1.replaceAll("[^a-zA-Z0-9]", "");
		
		Map<String, Object> deviceData = new HashMap<>();
		deviceData.put("dealerId", dealerId);
		deviceData.put("customerName", customerName);
		deviceData.put("customerPhone", customerPhone);
		deviceData.put("imei1", imei1);
		deviceData.put("imei2", imei2);
		deviceData.put("isLocked", true);
		deviceData.put("registeredAt", System.currentTimeMillis());
		
		database.child(deviceKey).setValue(deviceData)
		.addOnSuccessListener(aVoid -> {
			// Also send to PHP server
			ApiHelper.registerDevice(dealerId, customerName, customerPhone, imei1, imei2,
			new ApiHelper.ApiCallback() {
				@Override
				public void onSuccess(String response) {
					runOnUiThread(() -> {
						Toast.makeText(MainActivity.this,
						"Device registered successfully", Toast.LENGTH_SHORT).show();
						startLockService();
						startLockActivity();
						finish();
					});
				}
				
				@Override
				public void onError(String error) {
					runOnUiThread(() -> {
						// Still proceed even if server registration fails
						Toast.makeText(MainActivity.this,
						"Device registered to Firebase", Toast.LENGTH_SHORT).show();
						startLockService();
						startLockActivity();
						finish();
					});
				}
			});
		})
		.addOnFailureListener(e -> {
			tvStatus.setText("Registration failed: " + e.getMessage());
			btnRegister.setEnabled(true);
		});
	}
	
	private void startLockService() {
		Intent serviceIntent = new Intent(this, LockService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
			} else {
			startService(serviceIntent);
		}
	}
	
	private void startLockActivity() {
		Intent intent = new Intent(this, LockActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
			if (resultCode == RESULT_OK) {
				String dealerId = etDealerId.getText().toString().trim();
				String customerName = etCustomerName.getText().toString().trim();
				String customerPhone = etCustomerPhone.getText().toString().trim();
				registerDevice(dealerId, customerName, customerPhone);
				} else {
				Toast.makeText(this, "Device Admin permission is required", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getIMEI();
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		if (prefs.getBoolean("isLocked", false)) {
			// Don't allow back press if locked
			return;
		}
		super.onBackPressed();
	}
}