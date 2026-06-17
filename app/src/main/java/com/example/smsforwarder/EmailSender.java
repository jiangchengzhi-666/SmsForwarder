package com.example.smsforwarder;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * 邮件发送工具类
 * 通过 SMTP 协议发送邮件，支持 QQ 邮箱、163 邮箱等
 *
 * QQ邮箱配置方法：
 * 1. 登录 mail.qq.com → 设置 → 账户
 * 2. 找到"POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务"
 * 3. 开启"POP3/SMTP服务"
 * 4. 生成一个授权码（不是QQ密码）
 */
public class EmailSender {

    /**
     * 发送邮件
     *
     * @param smtpHost   SMTP服务器地址，如 "smtp.qq.com"
     * @param smtpPort   SMTP端口，QQ邮箱用 465(SSL) 或 587(TLS)
     * @param sender     发件人邮箱
     * @param authCode   授权码（不是登录密码）
     * @param receiver   收件人邮箱
     * @param subject    邮件主题
     * @param body       邮件正文
     * @throws MessagingException 发送失败时抛出异常
     */
    public static void send(String smtpHost, int smtpPort,
                           String sender, String authCode,
                           String receiver, String subject, String body)
            throws MessagingException {

        // 1. 配置 SMTP 连接参数
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");              // 需要登录认证
        props.put("mail.smtp.host", smtpHost);            // SMTP服务器
        props.put("mail.smtp.port", String.valueOf(smtpPort)); // 端口

        // 根据端口自动选择加密方式
        if (smtpPort == 465) {
            // 465端口 = SSL加密
            props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else if (smtpPort == 587) {
            // 587端口 = STARTTLS加密
            props.put("mail.smtp.starttls.enable", "true");
        }

        // 2. 创建邮件会话（带认证）
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, authCode);
            }
        });

        // 3. 构建邮件内容
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));          // 发件人
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(receiver));                // 收件人
        message.setSubject(subject);                           // 主题
        message.setText(body);                                 // 正文

        // 4. 发送
        Transport.send(message);
    }

    /**
     * 发送验证码转发邮件
     */
    public static void sendCode(ConfigManager config,
                                String senderPhone, String code, String originalMsg)
            throws MessagingException {
        String subject = "📱 短信验证码转发";
        String body = "来源号码：" + senderPhone + "\n\n"
                + "验证码：" + code + "\n\n"
                + "原始短信：\n" + originalMsg + "\n\n"
                + "---\n来自短信转发App";
        send(config.getSmtpServer(), config.getSmtpPort(),
                config.getSenderEmail(), config.getAuthCode(),
                config.getReceiverEmail(), subject, body);
    }

    /**
     * 发送心跳邮件
     */
    public static void sendHeartbeat(ConfigManager config)
            throws MessagingException {
        String subject = "💓 短信转发服务心跳 - " +
                new java.text.SimpleDateFormat("MM-dd HH:mm",
                java.util.Locale.getDefault()).format(new java.util.Date());
        String body = "短信转发服务正在正常运行中。\n\n"
                + "时间：" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n"
                + "如需远程重启，请回复邮件，主题写：restart\n\n"
                + "---\n来自短信转发App";
        send(config.getSmtpServer(), config.getSmtpPort(),
                config.getSenderEmail(), config.getAuthCode(),
                config.getReceiverEmail(), subject, body);
    }

    /**
     * 发送远程指令执行结果通知邮件
     */
    public static void sendCommandResult(ConfigManager config, String command, String result)
            throws MessagingException {
        String subject = "✅ 远程指令执行结果";
        String body = "收到指令：" + command + "\n\n"
                + "执行结果：" + result + "\n\n"
                + "时间：" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date()) + "\n\n"
                + "---\n来自短信转发App";
        send(config.getSmtpServer(), config.getSmtpPort(),
                config.getSenderEmail(), config.getAuthCode(),
                config.getReceiverEmail(), subject, body);
    }
}
