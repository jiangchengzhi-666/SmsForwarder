package com.example.smsforwarder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private EditText etSmtpServer, etSmtpPort, etSenderEmail, etAuthCode, etReceiverEmail;
    private Switch swForward, swHeartbeat;
    private Button btnSave, btnStart, btnStop;
    private TextView tvStatus;
    private ConfigManager config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        config = new ConfigManager(this);
        initViews();
        loadConfig();
        requestPermissions();
    }

    private void initViews() {
        etSmtpServer = findViewById(R.id.et_smtp_server);
        etSmtpPort = findViewById(R.id.et_smtp_port);
        etSenderEmail = findViewById(R.id.et_sender_email);
        etAuthCode = findViewById(R.id.et_auth_code);
        etReceiverEmail = findViewById(R.id.et_receiver_email);
        swForward = findViewById(R.id.sw_forward);
        swHeartbeat = findViewById(R.id.sw_heartbeat);
        btnSave = findViewById(R.id.btn_save);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvStatus = findViewById(R.id.tv_status);
        btnSave.setOnClickListener(v -> saveConfig());
        btnStart.setOnClickListener(v -> startService());
        btnStop.setOnClickListener(v -> stopService());
    }

    private void loadConfig() {
        etSmtpServer.setText(config.getSmtpServer());
        etSmtpPort.setText(String.valueOf(config.getSmtpPort()));
        etSenderEmail.setText(config.getSenderEmail());
        etAuthCode.setText(config.getAuthCode());
        etReceiverEmail.setText(config.getReceiverEmail());
        swForward.setChecked(config.isForwardEnabled());
        swHeartbeat.setChecked(config.isHeartbeatEnabled());
    }

    private void saveConfig() {
        config.setSmtpServer(etSmtpServer.getText().toString().trim());
        try { config.setSmtpPort(Integer.parseInt(etSmtpPort.getText().toString().trim())); }
        catch (NumberFormatException e) { config.setSmtpPort(465); }
        config.setSenderEmail(etSenderEmail.getText().toString().trim());
        config.setAuthCode(etAuthCode.getText().toString().trim());
        config.setReceiverEmail(etReceiverEmail.getText().toString().trim());
        config.setForwardEnabled(swForward.isChecked());
        config.setHeartbeatEnabled(swHeartbeat.isChecked());
        Toast.makeText(this, "Config saved", Toast.LENGTH_SHORT).show();
    }

    private void startService() {
        saveConfig();
        Intent intent = new Intent(this, SmsService.class);
        intent.setAction(SmsService.ACTION_START_HEARTBEAT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        tvStatus.setText("Status: Service started");
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

    private void stopService() {
        Intent intent = new Intent(this, SmsService.class);
        intent.setAction(SmsService.ACTION_STOP);
        startService(intent);
        tvStatus.setText("Status: Service stopped");
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 100);
            }
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
        }
    }
}
