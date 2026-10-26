package com.mrstopup.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "MRSPayReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null) return;

        StringBuilder fullMessage = new StringBuilder();
        String senderNumber = "";

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }

            if (sms != null) {
                senderNumber = sms.getDisplayOriginatingAddress();
                fullMessage.append(sms.getMessageBody());
            }
        }

        String finalMessage = fullMessage.toString();
        String sender = senderNumber;

        // Filter only financial SMS (bKash, Nagad, Rocket, 16216, TrxID, TxnID)
        String sLower = sender.toLowerCase();
        String msgLower = finalMessage.toLowerCase();
        boolean isRelevant = sLower.contains("bkash") || msgLower.contains("bkash") ||
                             sLower.contains("nagad") || msgLower.contains("nagad") ||
                             sLower.contains("16216") || msgLower.contains("rocket") ||
                             msgLower.contains("trxid") || msgLower.contains("txnid");

        if (isRelevant) {
            SharedPreferences prefs = context.getSharedPreferences("MRSPayPrefs", Context.MODE_PRIVATE);
            String targetUrl = prefs.getString("server_url", "https://pay.mrstopup.com/api.php?action=sms-push");
            String secretKey = prefs.getString("secret_key", "mrs_pay_secret_token_8899");

            new Thread(() -> sendToServer(targetUrl, secretKey, sender, finalMessage)).start();
        }
    }

    private void sendToServer(String targetUrl, String secretKey, String sender, String message) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-Gateway-Secret", secretKey);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            JSONObject json = new JSONObject();
            json.put("sender", sender);
            json.put("message", message);
            json.put("secret", secretKey);

            byte[] postData = json.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData, 0, postData.length);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Gateway Sync Status: " + code);
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error pushing SMS: " + e.getMessage());
        }
    }
}
