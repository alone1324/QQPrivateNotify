package com.example.qqprivatenotify;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;

/** Independent switches for private and group notifications. */
final class WhitelistConfig {
    private static final String PREFS = "settings";
    static final String PRIVATE_ENABLED = "private_enabled";
    static final String GROUP_ENABLED = "group_enabled";
    private static volatile SharedPreferences remotePreferences;

    private WhitelistConfig() {}

    static void setRemotePreferences(SharedPreferences preferences) { remotePreferences = preferences; }

    static boolean isConfigured(Context context) { return context != null; }
    static boolean shouldShowSource(Context context, Object source, int type) { return enabled(context, type); }
    static boolean shouldShow(Context context, Notification notification, int type) { return enabled(context, type); }
    static boolean shouldShow(Context context, Notification notification, int type, java.util.Set<String> ignored) { return enabled(context, type); }
    static boolean shouldShowUnknown(Context context) { return read(context).groupEnabled; }
    static void collectObjectIds(Object source, java.util.Set<String> out) { }

    private static boolean enabled(Context context, int type) {
        Config c = read(context);
        if (type == ConversationClassifier.GROUP) return c.groupEnabled;
        if (type == ConversationClassifier.PRIVATE) return c.privateEnabled;
        return true;
    }

    private static Config read(Context context) {
        SharedPreferences remote = remotePreferences;
        if (remote != null) {
            try {
                if (remote.contains(PRIVATE_ENABLED) || remote.contains(GROUP_ENABLED)) {
                    return new Config(remote.getBoolean(PRIVATE_ENABLED, true), remote.getBoolean(GROUP_ENABLED, false));
                }
            }
            catch (Throwable ignored) { }
        }
        if (context == null) return new Config(true, false);
        try (android.database.Cursor c = context.getContentResolver().query(
                android.net.Uri.parse("content://com.example.qqprivatenotify.config/settings"), null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int pi = c.getColumnIndex(PRIVATE_ENABLED), gi = c.getColumnIndex(GROUP_ENABLED);
                if (pi >= 0 || gi >= 0) {
                    return new Config(pi < 0 || c.getInt(pi) != 0, gi >= 0 && c.getInt(gi) != 0);
                }
            }
        } catch (Throwable ignored) { }
        try {
            Context module = context.createPackageContext("com.example.qqprivatenotify", Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences p = module.getSharedPreferences(PREFS, 0);
            return new Config(p.getBoolean(PRIVATE_ENABLED, true), p.getBoolean(GROUP_ENABLED, false));
        } catch (Throwable ignored) {
            try {
                SharedPreferences p = context.getSharedPreferences(PREFS, 0);
                return new Config(p.getBoolean(PRIVATE_ENABLED, true), p.getBoolean(GROUP_ENABLED, false));
            } catch (Throwable ignoredAgain) { return new Config(true, false); }
        }
    }

    private static final class Config {
        final boolean privateEnabled, groupEnabled;
        Config(boolean privateEnabled, boolean groupEnabled) { this.privateEnabled = privateEnabled; this.groupEnabled = groupEnabled; }
    }
}
