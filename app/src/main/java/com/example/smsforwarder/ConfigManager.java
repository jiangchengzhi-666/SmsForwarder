package com.example.smsforwarder;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置管理器
 * 负责存储和读取所有用户配置（SMTP、邮箱、开关等）
 * 数据保存在 SharedPreferences 中，App 卸载前一直存在
 */
public class ConfigManager {

    private static final String PREF_NAME = "sms_forwarder_config";
    private final SharedPreferences prefs;

    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ========== SMTP 配置 ==========

    public String getSmtpServer() {
        return prefs.getString("smtp_server", "smtp.qq.com");
    }

    public void setSmtpServer(String server) {
        prefs.edit().putString("smtp_server", server).apply();
    }

    public int getSmtpPort() {
        return prefs.getInt("smtp_port", 465);
    }

    public void setSmtpPort(int port) {
        prefs.edit().putInt("smtp_port", port).apply();
    }

    public String getSenderEmail() {
        return prefs.getString("sender_email", "");
    }

    public void setSenderEmail(String email) {
        prefs.edit().putString("sender_email", email).apply();
    }

    public String getAuthCode() {
        return prefs.getString("auth_code", "");
    }

    public void setAuthCode(String code) {
        prefs.edit().putString("auth_code", code).apply();
    }

    // ========== 收件配置 ==========

    public String getReceiverEmail() {
        return prefs.getString("receiver_email", "");
    }

    public void setReceiverEmail(String email) {
        prefs.edit().putString("receiver_email", email).apply();
    }

    // ========== 功能开关 ==========

    public boolean isForwardEnabled() {
        return prefs.getBoolean("forward_enabled", false);
    }

    public void setForwardEnabled(boolean enabled) {
        prefs.edit().putBoolean("forward_enabled", enabled).apply();
    }

    public boolean isHeartbeatEnabled() {
        return prefs.getBoolean("heartbeat_enabled", false);
    }

    public void setHeartbeatEnabled(boolean enabled) {
        prefs.edit().putBoolean("heartbeat_enabled", enabled).apply();
    }

    // ========== 心跳间隔（分钟）==========

    public int getHeartbeatInterval() {
        return prefs.getInt("heartbeat_interval", 30);
    }

    public void setHeartbeatInterval(int interval) {
        prefs.edit().putInt("heartbeat_interval", interval).apply();
    }
}
