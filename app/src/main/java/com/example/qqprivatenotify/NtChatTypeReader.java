package com.example.qqprivatenotify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class NtChatTypeReader {
    private NtChatTypeReader() {}

    static int read(Object source, boolean notifyItem) throws ReflectiveOperationException {
        if (source == null) return 0;
        if (notifyItem) source = readProperty(source, "msgInfo", "getMsgInfo");
        if (source == null) return 0;
        Object value = readProperty(source, "chatType", "getChatType");
        return value instanceof Integer ? (Integer) value : 0;
    }

    private static Object readProperty(Object source, String fieldName, String getterName)
            throws ReflectiveOperationException {
        for (Class<?> type = source.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (NoSuchFieldException ignored) {
                // Some host builds expose only the public nativeinterface getter.
            }
        }
        Method getter = source.getClass().getMethod(getterName);
        getter.setAccessible(true);
        return getter.invoke(source);
    }
}
