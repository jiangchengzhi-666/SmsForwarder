package com.example.smsforwarder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 验证码提取器
 * 从短信内容中智能提取验证码，支持多种格式：
 * - "验证码：123456"
 * - "验证码 123456"
 * - "验证码为123456"
 * - "动态码：123456"
 * - "code:123456"
 * - "code is 123456"
 * - "校验码：123456"
 * - 纯数字短信（兜底）
 */
public class CodeExtractor {

    // 验证码关键词列表（不区分大小写）
    private static final String[] KEYWORDS = {
        "验证码", "动态码", "动态密码", "校验码", "安全码",
        "提取码", "确认码", "认证码", "授权码", "短信码",
        "code", "CODE", "pin", "PIN"
    };

    // 构建正则：关键词 + 可选分隔符 + 4~8位数字
    private static final Pattern[] CODE_PATTERNS;

    static {
        // 把关键词用 | 连接，注意中文关键词需要转义
        StringBuilder keywordGroup = new StringBuilder();
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (i > 0) keywordGroup.append("|");
            keywordGroup.append(Pattern.quote(KEYWORDS[i]));
        }

        // 模式1：关键词后直接跟数字（可能有空格、冒号、逗号等分隔）
        // 例如："验证码：123456" "验证码 123456" "code:123456"
        String pattern1 = "(?i)(" + keywordGroup + ")[:：\s\-_.]?\s*(\d{4,8})";

        // 模式2：关键词 + "为/是" + 数字
        // 例如："验证码为123456" "验证码是123456"
        String pattern2 = "(?i)(" + keywordGroup + ")[为是]\s*(\d{4,8})";

        // 模式3：关键词 + "is/are" + 数字
        // 例如："code is 123456"
        String pattern3 = "(?i)(" + keywordGroup + ")\s+(?:is|are)\s+(\d{4,8})";

        CODE_PATTERNS = new Pattern[]{
            Pattern.compile(pattern1),
            Pattern.compile(pattern2),
            Pattern.compile(pattern3)
        };
    }

    // 兜底模式：整条短信如果几乎全是数字，直接提取
    private static final Pattern PURE_DIGIT = Pattern.compile("(\d{4,8})");

    /**
     * 从短信内容中提取验证码
     * @param message 短信正文
     * @return 提取到的验证码，提取失败返回 null
     */
    public static String extract(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        // 优先用关键词模式匹配
        for (Pattern pattern : CODE_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(2); // 第2个捕获组是数字
            }
        }

        // 兜底：如果短信很短且包含数字，尝试提取
        // 避免误匹配长短信中的无关数字
        if (message.length() <= 30) {
            Matcher matcher = PURE_DIGIT.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
