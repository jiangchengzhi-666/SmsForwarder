package com.example.smsforwarder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeExtractor {
    private static final String[] KEYWORDS = {
        "verification code", "dynamic code", "code", "pin", "PIN"
    };

    private static final Pattern[] CODE_PATTERNS;

    static {
        StringBuilder keywordGroup = new StringBuilder();
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (i > 0) keywordGroup.append("|");
            keywordGroup.append(Pattern.quote(KEYWORDS[i]));
        }

        String pattern1 = "(?i)(" + keywordGroup + ")[:\\s\\-_.]?\\s*(\\d{4,8})";
        String pattern2 = "(?i)(" + keywordGroup + ")\\s+(?:is|are)\\s+(\\d{4,8})";

        CODE_PATTERNS = new Pattern[]{
            Pattern.compile(pattern1),
            Pattern.compile(pattern2)
        };
    }

    private static final Pattern PURE_DIGIT = Pattern.compile("(\\d{4,8})");

    public static String extract(String message) {
        if (message == null || message.isEmpty()) return null;
        for (Pattern pattern : CODE_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) return matcher.group(2);
        }
        if (message.length() <= 30) {
            Matcher matcher = PURE_DIGIT.matcher(message);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }
}
