package com.example.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * 短信广播接收器
 * 监听系统 SMS_RECEIVED 广播，收到短信后启动前台服务处理
 *
 * 工作流程：
 * 1. 系统收到短信 → 发出 SMS_RECEIVED 广播
 * 2. 本接收器收到广播 → 提取短信内容
 * 3. 启动前台 Service，把短信数据传过去
 * 4. Service 负责提取验证码 + 发邮件（避免在广播里做耗时操作）
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 检查是否是短信广播
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        // 检查转发功能是否启用
        ConfigManager config = new ConfigManager(context);
        if (!config.isForwardEnabled()) {
            Log.d(TAG, "短信转发功能未启用，忽略");
            return;
        }

        // 从广播 Intent 中提取短信数据
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        String format = bundle.getString("format");

        // 拼接所有 PDU 片段（长短信会被拆成多条）
        StringBuilder fullMessage = new StringBuilder();
        String senderPhone = "";

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }

            if (senderPhone.isEmpty()) {
                senderPhone = sms.getOriginatingAddress();
            }
            fullMessage.append(sms.getMessageBody());
        }

        String messageBody = fullMessage.toString();
        Log.d(TAG, "收到来自 " + senderPhone + " 的短信：" + messageBody);

        // 尝试提取验证码
        String code = CodeExtractor.extract(messageBody);

        // 如果提取到了验证码，或者转发全部短信的模式，就启动服务
        if (code != null) {
            Log.d(TAG, "提取到验证码：" + code);
        }

        // 启动前台服务来处理发送邮件（不能在广播接收器里做耗时操作）
        Intent serviceIntent = new Intent(context, SmsService.class);
        serviceIntent.setAction(SmsService.ACTION_FORWARD_SMS);
        serviceIntent.putExtra(SmsService.EXTRA_SENDER, senderPhone);
        serviceIntent.putExtra(SmsService.EXTRA_MESSAGE, messageBody);
        serviceIntent.putExtra(SmsService.EXTRA_CODE, code);

        // Android 8.0+ 必须用 startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
