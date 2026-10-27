package com.mrstopup.smsforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HeartbeatService extends Service {
    private static final String CHANNEL_ID = "mrs_pay_sync_channel";
    private static final int NOTIF_ID = 8899;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pingRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, createNotification());

        pingRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartbeatPing();
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(pingRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MRS PAY Background Sync",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps SMS reader active in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MRS PAY সার্ভিস চালু আছে")
                .setContentText("● ব্যাকগ্রাউন্ডে SMS সিঙ্ক ও পেমেন্ট সার্ভিস সক্রিয়")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void sendHeartbeatPing() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MRSPayPrefs", Context.MODE_PRIVATE);
                String targetUrl = prefs.getString("server_url", "https://pay.mrstopup.com/api.php?action=sms-push");
                String secretKey = prefs.getString("secret_key", "mrs_pay_secret_token_8899");

                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("X-Gateway-Secret", secretKey);
                conn.setConnectTimeout(6000);
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("ping", true);
                json.put("secret", secretKey);

                byte[] postData = json.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData, 0, postData.length);
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && pingRunnable != null) {
            handler.removeCallbacks(pingRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
