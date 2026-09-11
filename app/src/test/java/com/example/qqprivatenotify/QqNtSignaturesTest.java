package com.example.qqprivatenotify;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class QqNtSignaturesTest {
    private static final String FACADE = "com.tencent.qqnt.notification.NotificationFacade";
    private static final String RESULT = FACADE + "$a$a";
    private static final String TRACKER = "com.tencent.qqnt.notification.trace.INotifyTracker";
    private static final String SETTINGS = "com.tencent.qqnt.global.settings.notification.a";
    private static final String APP = "mqq.app.AppRuntime";
    private static final String RECENT =
            "com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo";
    private static final String COMMON =
            "com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo";
    private static final String ITEM =
            "com.tencent.qqnt.kernel.nativeinterface.MsgNotifyItem";
    private static final String ELEMENT = "com.tencent.qqnt.notification.struct.d";

    @Test public void currentNotificationEntryPointsHaveDirectMessageArguments() {
        assertEquals(1, match(APP, RECENT, COMMON, "boolean"));
        assertEquals(1, match(APP, ITEM, "boolean"));
    }

    @Test public void legacyNotificationEntriesRetainTheirRecentContactArgument() {
        assertEquals(3, match(APP, ELEMENT, COMMON, RECENT));
        assertEquals(3, match(APP, ELEMENT, COMMON, RECENT, "boolean"));
        assertEquals(3, match(APP, "obfuscated.a", COMMON, RECENT, "boolean"));
    }

    @Test public void nonVoidMethodsCannotBeSuppressed() {
        for (String returnType : new String[]{"boolean", "java.lang.Boolean", "java.lang.Void",
                "java.lang.Object", "android.app.Notification", "int", null}) {
            assertEquals(-1, QqNtSignatures.recentArgumentIndex(returnType,
                    new String[]{APP, RECENT, COMMON, "boolean"}));
            assertEquals(-1, QqNtSignatures.recentArgumentIndex(returnType,
                    new String[]{APP, ITEM, "boolean"}));
            assertEquals(-1, QqNtSignatures.recentArgumentIndex(returnType,
                    new String[]{APP, ELEMENT, COMMON, RECENT, "boolean"}));
        }
    }

    @Test public void aggregateReadStatusRefreshCannotMatch() {
        assertEquals(-1, match(ITEM, RECENT, RECENT));
        assertEquals(-1, match(APP, ITEM, RECENT));
        assertEquals(-1, match(APP, RECENT, RECENT));
    }

    @Test public void recentInfoBuildersAndBoxedFlagsCannotMatch() {
        assertEquals(-1, match(APP, RECENT, "java.lang.Boolean"));
        assertEquals(-1, match(APP, RECENT, "java.lang.Boolean", "int"));
        assertEquals(-1, match(APP, RECENT, COMMON, "java.lang.Boolean"));
        assertEquals(-1, match(APP, ITEM, "java.lang.Boolean"));
        assertEquals(-1, match(APP, ELEMENT, COMMON, RECENT, "java.lang.Boolean"));
    }

    @Test public void missingOrExtraFlagsCannotMatchCurrentEntryPoints() {
        assertEquals(-1, match(APP, ITEM));
        assertEquals(-1, match(APP, RECENT, COMMON));
        assertEquals(-1, match(APP, ITEM, "boolean", "boolean"));
        assertEquals(-1, match(APP, RECENT, COMMON, "boolean", "boolean"));
        assertEquals(-1, match(APP, ELEMENT, COMMON, RECENT, "boolean", "boolean"));
    }

    @Test public void reorderedAndUnrelatedTypesCannotMatch() {
        assertEquals(-1, match(APP, COMMON, RECENT, "boolean"));
        assertEquals(-1, match(APP, ELEMENT, RECENT, COMMON, "boolean"));
        assertEquals(-1, match(APP, ITEM, COMMON, "boolean"));
        assertEquals(-1, match("android.app.Application", RECENT, COMMON, "boolean"));
        assertEquals(-1, match(APP,
                "com.tencent.qqnt.kernelpublic.nativeinterface.RecentContactInfo", COMMON, "boolean"));
        assertEquals(-1, match(APP, "RecentContactInfo", COMMON, "boolean"));
    }

    @Test public void legacyElementMustBeAnObjectRatherThanPrimitiveOrArray() {
        for (String element : new String[]{"void", "boolean", "byte", "char", "short", "int",
                "long", "float", "double", "[I", "[Ljava.lang.Object;", "java.lang.Object[]", "", null}) {
            assertEquals(-1, match(APP, element, COMMON, RECENT));
            assertEquals(-1, match(APP, element, COMMON, RECENT, "boolean"));
        }
    }

    @Test public void absentParameterDataDoesNotMatch() {
        assertEquals(-1, QqNtSignatures.recentArgumentIndex("void", null));
        assertEquals(-1, match());
        assertEquals(-1, match((String) null));
    }

    @Test public void asyncBuildersMatchFullVerifiedSignatures() {
        assertEquals(2, builder(FACADE, APP, ITEM, "boolean", TRACKER, SETTINGS));
        assertEquals(2, builder(FACADE, APP, RECENT, COMMON, "boolean", TRACKER, SETTINGS));
    }

    @Test public void partialOrUnrelatedAsyncBuildersDoNotMatch() {
        assertEquals(-1, builder(FACADE, APP, ITEM, "boolean"));
        assertEquals(-1, builder(FACADE, APP, ITEM, "java.lang.Boolean", TRACKER, SETTINGS));
        assertEquals(-1, builder(FACADE, APP, RECENT, COMMON, "boolean", SETTINGS, TRACKER));
        assertEquals(-1, builder(FACADE, APP, ITEM, "boolean", "java.lang.Object", SETTINGS));
        assertEquals(-1, builder(FACADE, APP, ITEM, "boolean", TRACKER, "java.lang.Object"));
        assertEquals(-1, builder(FACADE, APP, RECENT, "boolean", TRACKER, SETTINGS));
        assertEquals(-1, builder(FACADE, APP, ITEM, COMMON, "boolean", TRACKER, SETTINGS));
        assertEquals(-1, QqNtSignatures.syntheticBuilderArgumentIndex(RESULT + "Other", FACADE,
                new String[]{FACADE, APP, ITEM, "boolean", TRACKER, SETTINGS}));
        assertEquals(-1, QqNtSignatures.syntheticBuilderArgumentIndex("void", FACADE,
                new String[]{FACADE, APP, ITEM, "boolean", TRACKER, SETTINGS}));
        assertEquals(-1, QqNtSignatures.syntheticBuilderArgumentIndex(null, FACADE, null));
    }

    @Test public void onlyVoidNotificationPostSignaturesMatch() {
        assertEquals(-1, post("void", "android.app.Notification", "int"));
        assertEquals(1, post("void", "java.lang.String", "android.app.Notification", "int"));
        assertEquals(-1, post("boolean", "java.lang.String", "android.app.Notification", "int"));
        assertEquals(-1, post("void", "java.lang.String", "android.app.Notification", "java.lang.Integer"));
        assertEquals(-1, post("void", "int", "android.app.Notification"));
        assertEquals(-1, post("void", "java.lang.String", "android.app.Notification"));
        assertEquals(-1, post("void"));
    }

    private static int builder(String... params) {
        return QqNtSignatures.syntheticBuilderArgumentIndex(RESULT, FACADE, params);
    }

    private static int post(String result, String... params) {
        return QqNtSignatures.postNotificationArgumentIndex(result, params);
    }

    private static int match(String... params) {
        return QqNtSignatures.recentArgumentIndex("void", params);
    }
}
