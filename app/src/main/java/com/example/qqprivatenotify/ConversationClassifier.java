package com.example.qqprivatenotify;

import android.app.Notification;
import android.os.Bundle;
import android.content.Intent;
import java.util.HashMap;
import java.util.Map;

final class ConversationClassifier {
    static final int PRIVATE = 1;
    static final int GROUP = 2;
    static final String[] KEYS = {"uintype", "uin_type", "chatType", "chat_type",
            "android.isGroupConversation", "is_group", "isGroup", "is_group_chat"};

    private ConversationClassifier() {}

    static int classify(Map<String, ?> fields) {
        int result = 0;
        // QQ's legacy UIN type and NT chat type use different numeric values.
        for (String key : new String[]{"uintype", "uin_type"}) {
            String type = scalar(fields.get(key));
            if ("0".equals(type)) result |= PRIVATE;
            else if ("1".equals(type) || "3000".equals(type)) result |= GROUP;
        }
        for (String key : new String[]{"chatType", "chat_type"}) {
            String type = scalar(fields.get(key));
            if ("1".equals(type) || "100".equals(type)) result |= PRIVATE;
            else if ("2".equals(type)) result |= GROUP;
        }
        for (String key : new String[]{"android.isGroupConversation", "is_group", "isGroup", "is_group_chat"}) {
            String value = scalar(fields.get(key));
            if ("true".equalsIgnoreCase(value) || "1".equals(value)) result |= GROUP;
            else if (!"android.isGroupConversation".equals(key)
                    && ("false".equalsIgnoreCase(value) || "0".equals(value))) result |= PRIVATE;
        }
        return result;
    }

    static boolean shouldBlock(int classification) {
        // Mixed/contradictory metadata and unknown conversations are allowed through.
        return classification == GROUP;
    }

    static int classifyNotification(Notification notification) {
        if (notification == null) return 0;
        int result = classifyBundle(notification.extras, 0);
        if (result == 0 && notification.extras != null) {
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            if (title != null && title.length() > 0) result = GROUP;
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static int classifyBundle(Bundle bundle, int depth) {
        if (bundle == null || depth > 2) return 0;
        Map<String, Object> fields = new HashMap<>();
        for (String key : KEYS) if (bundle.containsKey(key)) fields.put(key, bundle.get(key));
        int result = classify(fields);
        for (String key : new String[]{"intent", "extras", "extra", "data", "param", "params", "contact", "session", "bundle"}) {
            Object nested = bundle.get(key);
            if (nested instanceof Bundle) result |= classifyBundle((Bundle) nested, depth + 1);
            else if (nested instanceof Intent) result |= classifyBundle(((Intent) nested).getExtras(), depth + 1);
        }
        return result;
    }

    private static String scalar(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Boolean
                || value instanceof String ? value.toString() : "";
    }
}
