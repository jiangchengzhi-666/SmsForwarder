package com.example.smsforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SubjectTerm;

public class SmsService extends Service {
    private static final String TAG = "SmsService";
    private static final String CHANNEL_ID = "sms_forwarder_channel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_FORWARD_SMS = "action_forward_sms";
    public static final String ACTION_START_HEARTBEAT = "action_start_heartbeat";
    public static final String ACTION_STOP = "action_stop";
    public static final String EXTRA_SENDER = "extra_sender";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_CODE = "extra_code";
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private Thread heartbeatThread;
    private volatile boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundWithNotification("SMS forwarding service started");
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { startHeartbeatIfNeeded(); return START_STICKY; }
        String action = intent.getAction();
        if (action == null) { startHeartbeatIfNeeded(); return START_STICKY; }
        switch (action) {
            case ACTION_FORWARD_SMS:
                handleForwardSms(intent.getStringExtra(EXTRA_SENDER),
                    intent.getStringExtra(EXTRA_MESSAGE), intent.getStringExtra(EXTRA_CODE));
                break;
            case ACTION_START_HEARTBEAT: startHeartbeatIfNeeded(); break;
            case ACTION_STOP: stopSelf(); break;
        }
        return START_STICKY;
    }

    private void handleForwardSms(String sender, String message, String code) {
        if (code == null || code.isEmpty()) {
            Log.d(TAG, "No code found, skip");
            updateNotification("Waiting for verification code...");
            return;
        }
        updateNotification("Forwarding code: " + code);
        executor.execute(() -> {
            try {
                ConfigManager config = new ConfigManager(SmsService.this);
                EmailSender.sendCode(config, sender, code, message);
                Log.d(TAG, "Code email sent");
                updateNotification("Code " + code + " forwarded");
            } catch (Exception e) {
                Log.e(TAG, "Email failed", e);
                updateNotification("Email failed: " + e.getMessage());
            }
        });
    }

    private void startHeartbeatIfNeeded() {
        ConfigManager config = new ConfigManager(this);
        if (!config.isHeartbeatEnabled()) return;
        if (heartbeatThread != null && heartbeatThread.isAlive()) return;
        isRunning = true;
        heartbeatThread = new Thread(() -> {
            while (isRunning) {
                try {
                    ConfigManager cfg = new ConfigManager(SmsService.this);
                    if (!cfg.isHeartbeatEnabled()) break;
                    int interval = cfg.getHeartbeatInterval();
                    for (int i = 0; i < interval * 60 && isRunning; i++) Thread.sleep(1000);
                    if (!isRunning) break;
                    try {
                        EmailSender.sendHeartbeat(cfg);
                        Log.d(TAG, "Heartbeat sent");
                        updateNotification("Heartbeat OK");
                    } catch (Exception e) {
                        Log.e(TAG, "Heartbeat failed", e);
                        updateNotification("Heartbeat failed");
                    }
                    checkRemoteCommands(cfg);
                } catch (InterruptedException e) { break; }
            }
        }, "HeartbeatThread");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void checkRemoteCommands(ConfigManager config) {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", "imap.qq.com");
            props.put("mail.imaps.port", "993");
            props.put("mail.imaps.ssl.enable", "true");
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect("imap.qq.com", config.getSenderEmail(), config.getAuthCode());
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            javax.mail.Message[] msgs = inbox.search(new SubjectTerm("restart"));
            for (javax.mail.Message msg : msgs) {
                if (msg.isSet(javax.mail.Flags.Flag.SEEN)) continue;
                String subject = msg.getSubject();
                String result = executeCommand(subject);
                msg.setFlag(javax.mail.Flags.Flag.SEEN, true);
                EmailSender.sendCommandResult(config, subject, result);
            }
            inbox.close(true);
            store.close();
        } catch (Exception e) { Log.e(TAG, "Remote cmd failed", e); }
    }

    private String executeCommand(String command) {
        if (command == null) return "Unknown";
        String cmd = command.trim().toLowerCase();
        if (cmd.contains("restart")) {
            Intent i = new Intent(this, SmsService.class);
            i.setAction(ACTION_START_HEARTBEAT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
            return "Service restarted";
        } else if (cmd.contains("status")) {
            ConfigManager c = new ConfigManager(this);
            return "Forward:" + (c.isForwardEnabled()?"ON":"OFF") + " Heartbeat:" + (c.isHeartbeatEnabled()?"ON":"OFF") + " Interval:" + c.getHeartbeatInterval() + "min";
        } else if (cmd.contains("heartbeat on")) {
            new ConfigManager(this).setHeartbeatEnabled(true);
            startHeartbeatIfNeeded();
            return "Heartbeat enabled";
        } else if (cmd.contains("heartbeat off")) {
            new ConfigManager(this).setHeartbeatEnabled(false);
            return "Heartbeat disabled";
        }
        return "Unknown. Try: restart, status, heartbeat on/off";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "SMS Forwarder", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("SMS forwarding notification");
            NotificationManager m = getSystemService(NotificationManager.class);
            if (m != null) m.createNotificationChannel(ch);
        }
    }

    private void startForegroundWithNotification(String text) {
        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        startForeground(NOTIFICATION_ID, b.setContentTitle("SMS Code Forwarder")
            .setContentText(text).setSmallIcon(android.R.drawable.ic_dialog_email)
            .setOngoing(true).build());
    }

    private void updateNotification(String text) { startForegroundWithNotification(text); }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        super.onDestroy(); isRunning = false;
        if (heartbeatThread != null) heartbeatThread.interrupt();
        executor.shutdown();
    }
}
