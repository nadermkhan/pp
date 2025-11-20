package com.emillocker.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiHelper {
	
	// Replace with your actual server URL
	private static final String BASE_URL = "https://your-server.com/api/";
	
	private static final OkHttpClient client = new OkHttpClient.Builder()
	.connectTimeout(30, TimeUnit.SECONDS)
	.readTimeout(30, TimeUnit.SECONDS)
	.writeTimeout(30, TimeUnit.SECONDS)
	.build();
	
	public interface ApiCallback {
		void onSuccess(String response);
		void onError(String error);
	}
	
	public static void registerDevice(String dealerId, String customerName, String customerPhone,
	String imei1, String imei2, ApiCallback callback) {
		RequestBody formBody = new FormBody.Builder()
		.add("action", "register")
		.add("dealer_id", dealerId)
		.add("customer_name", customerName)
		.add("customer_phone", customerPhone)
		.add("imei1", imei1)
		.add("imei2", imei2)
		.build();
		
		Request request = new Request.Builder()
		.url(BASE_URL + "device.php")
		.post(formBody)
		.build();
		
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onError(e.getMessage()));
			}
			
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				final String responseData = response.body().string();
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onSuccess(responseData));
			}
		});
	}
	
	public static void checkLockStatus(String imei1, ApiCallback callback) {
		Request request = new Request.Builder()
		.url(BASE_URL + "device.php?action=check&imei=" + imei1)
		.get()
		.build();
		
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onError(e.getMessage()));
			}
			
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				final String responseData = response.body().string();
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onSuccess(responseData));
			}
		});
	}
	
	public static void updateFCMToken(String imei1, String token, ApiCallback callback) {
		RequestBody formBody = new FormBody.Builder()
		.add("action", "update_token")
		.add("imei", imei1)
		.add("token", token)
		.build();
		
		Request request = new Request.Builder()
		.url(BASE_URL + "device.php")
		.post(formBody)
		.build();
		
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onError(e.getMessage()));
			}
			
			@Override
			public void onResponse(Call call, Response response) throws IOException {
				final String responseData = response.body().string();
				new Handler(Looper.getMainLooper()).post(() ->
				callback.onSuccess(responseData));
			}
		});
	}
}