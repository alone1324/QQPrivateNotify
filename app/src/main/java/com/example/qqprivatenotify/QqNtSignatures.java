package com.example.qqprivatenotify;

final class QqNtSignatures {
    static final String APP_RUNTIME = "mqq.app.AppRuntime";
    static final String RECENT_CONTACT_INFO =
            "com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo";
    static final String NOTIFICATION_COMMON_INFO =
            "com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo";
    static final String MSG_NOTIFY_ITEM =
            "com.tencent.qqnt.kernel.nativeinterface.MsgNotifyItem";
    static final String FACADE = "com.tencent.qqnt.notification.NotificationFacade";
    static final String TRACKER = "com.tencent.qqnt.notification.trace.INotifyTracker";
    static final String SETTINGS = "com.tencent.qqnt.global.settings.notification.a";

    private QqNtSignatures() {}

    static int recentArgumentIndex(String returnType, String[] params) {
        if (!"void".equals(returnType) || params == null || params.length < 3
                || !APP_RUNTIME.equals(params[0])) return -1;

        if (params.length == 3 && MSG_NOTIFY_ITEM.equals(params[1])
                && "boolean".equals(params[2])) return 1;

        if (params.length == 4 && RECENT_CONTACT_INFO.equals(params[1])
                && NOTIFICATION_COMMON_INFO.equals(params[2])
                && "boolean".equals(params[3])) return 1;

        if ((params.length == 4 || params.length == 5)
                && isObjectType(params[1])
                && NOTIFICATION_COMMON_INFO.equals(params[2])
                && RECENT_CONTACT_INFO.equals(params[3])
                && (params.length == 4 || "boolean".equals(params[4]))) return 3;

        return -1;
    }

    static int syntheticBuilderArgumentIndex(String returnType, String facadeName, String[] params) {
        if (!FACADE.equals(facadeName) || !(FACADE + "$a$a").equals(returnType)
                || params == null || (params.length != 6 && params.length != 7)
                || !facadeName.equals(params[0]) || !APP_RUNTIME.equals(params[1])) return -1;
        if (params.length == 6 && MSG_NOTIFY_ITEM.equals(params[2])
                && "boolean".equals(params[3]) && TRACKER.equals(params[4])
                && SETTINGS.equals(params[5])) return 2;
        if (params.length == 7 && RECENT_CONTACT_INFO.equals(params[2])
                && NOTIFICATION_COMMON_INFO.equals(params[3]) && "boolean".equals(params[4])
                && TRACKER.equals(params[5]) && SETTINGS.equals(params[6])) return 2;
        return -1;
    }

    static int postNotificationArgumentIndex(String returnType, String[] params) {
        if (!"void".equals(returnType) || params == null) return -1;
        if (params.length == 3 && "java.lang.String".equals(params[0])
                && "android.app.Notification".equals(params[1]) && "int".equals(params[2])) return 1;
        return -1;
    }

    private static boolean isObjectType(String type) {
        if (type == null || type.isEmpty() || type.startsWith("[")
                || type.endsWith("[]")) return false;
        return switch (type) {
            case "void", "boolean", "byte", "char", "short", "int", "long", "float", "double" -> false;
            default -> true;
        };
    }
}
