package com.example.remoteshutdown;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ShutdownService extends Service {

    private static final String CHANNEL_ID = "RemoteShutdownChannel";
    private Handler handler;
    private Runnable runnable;
    private DevicePolicyManager dpm;
    private ComponentName compName;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RemoteShutdown")
                .setContentText("Sunucu dinleniyor...")
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .build();
        startForeground(1, notification);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyDeviceAdminReceiver.class);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                checkServer();
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(runnable);
    }

    private void checkServer() {
        new Thread(() -> {
            try {
                URL url = new URL("https://www.beraterkek12.pythonanywhere.com/shutdown");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(result.toString());
                    if (json.has("action") && json.getString("action").equals("shutdown")) {
                        if (dpm.isAdminActive(compName)) {
                            dpm.lockNow();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Remote Shutdown Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
