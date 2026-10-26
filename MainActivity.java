package com.mrstopup.smsforwarder;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;

    private EditText editServerUrl;
    private EditText editSecretKey;
    private TextView textStatus;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("MRSPayPrefs", Context.MODE_PRIVATE);

        editServerUrl = findViewById(R.id.editServerUrl);
        editSecretKey = findViewById(R.id.editSecretKey);
        textStatus = findViewById(R.id.textStatus);
        Button btnSave = findViewById(R.id.btnSave);

        // Load saved values
        editServerUrl.setText(prefs.getString("server_url", "https://pay.mrstopup.com/api.php?action=sms-push"));
        editSecretKey.setText(prefs.getString("secret_key", "mrs_pay_secret_token_8899"));

        btnSave.setOnClickListener(v -> {
            String url = editServerUrl.getText().toString().trim();
            String key = editSecretKey.getText().toString().trim();
            prefs.edit().putString("server_url", url).putString("secret_key", key).apply();
            Toast.makeText(this, "সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
            updateStatus();
        });

        requestPermissionsIfNeed();
        updateStatus();
    }

    private void requestPermissionsIfNeed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
            }, SMS_PERMISSION_CODE);
        }
    }

    private void updateStatus() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        if (hasPermission) {
            textStatus.setText("● সার্ভিস চালু আছে। SMS স্বয়ংক্রিয়ভাবে সার্ভারে যাবে।");
            textStatus.setTextColor(0xFF10B981);
        } else {
            textStatus.setText("⚠️ SMS পড়ার পারমিশন দেওয়া হয়নি!");
            textStatus.setTextColor(0xFFEF4444);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            updateStatus();
        }
    }
}
