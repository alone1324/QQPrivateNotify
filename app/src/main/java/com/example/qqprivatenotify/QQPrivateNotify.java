package com.example.qqprivatenotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.libxposed.api.XposedModule;

public final class QQPrivateNotify extends XposedModule {
    private static final String QQ = "com.tencent.mobileqq";
    private static final String TAG = "QQPrivateNotify";
    private final Map<PendingIntent, Integer> conversations = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<PendingIntent, Integer> entry) {
            return size() > 512;
        }
    };
    private boolean qqProcess;
    private boolean installed;
    private final Set<Method> ntMethods = new HashSet<>();
    private final AtomicInteger decisions = new AtomicInteger();
    private final AtomicInteger readFailures = new AtomicInteger();
    private final AtomicInteger unknownPosts = new AtomicInteger();
    private final NotificationTrace notificationTrace = new NotificationTrace();
    private String processName;
    private volatile Context hostContext;

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        String process = param.getProcessName();
        qqProcess = QQ.equals(process) || process.startsWith(QQ + ":");
        processName = process;
        try { WhitelistConfig.setRemotePreferences(getRemotePreferences("settings")); } catch (Throwable ignored) { }
        if (!qqProcess) return;
        log(Log.INFO, TAG, "v1.1 loaded process=" + process + " api=" + getApiVersion());
        installSystemHooks();
        installLifecycleRetry();
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!qqProcess || !QQ.equals(param.getPackageName())) return;
        installNtHooks(param.getClassLoader(), "package-ready");
    }

    private void installLifecycleRetry() {
        try {
            hook(Application.class.getDeclaredMethod("attach", Context.class)).intercept(chain -> {
                Object result = chain.proceed();
                Context context = (Context) chain.getArg(0);
                if (QQ.equals(context.getPackageName())) { hostContext = context; installNtHooks(context.getClassLoader(), "attach"); }
                return result;
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            log(Log.WARN, TAG, "Attach retry unavailable: " + error.getClass().getSimpleName());
        }
        try {
            hook(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", Application.class))
                    .intercept(chain -> {
                        Application app = (Application) chain.getArg(0);
                        if (QQ.equals(app.getPackageName())) { hostContext = app; installNtHooks(app.getClassLoader(), "application-create"); }
                        Object result = chain.proceed();
                        if (QQ.equals(app.getPackageName())) installNtHooks(app.getClassLoader(), "application-created");
                        return result;
                    });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            log(Log.WARN, TAG, "Lifecycle retry unavailable: " + error.getClass().getSimpleName());
        }
    }

    private synchronized void installNtHooks(ClassLoader loader, String stage) {
        try {
            Class<?> facade = Class.forName(QqNtSignatures.FACADE, false, loader);
            for (Method method : facade.getDeclaredMethods()) {
                if (ntMethods.contains(method)) continue;
                Class<?>[] types = method.getParameterTypes();
                String[] names = new String[types.length];
                for (int i = 0; i < types.length; i++) names[i] = types[i].getName();
                int index = QqNtSignatures.recentArgumentIndex(method.getReturnType().getName(), names);
                int builderIndex = QqNtSignatures.syntheticBuilderArgumentIndex(
                        method.getReturnType().getName(), facade.getName(), names);
                int postIndex = QqNtSignatures.postNotificationArgumentIndex(method.getReturnType().getName(), names);
                if (builderIndex >= 0 && Modifier.isStatic(method.getModifiers())) {
                    installBuilderContext(method, builderIndex, names[builderIndex]);
                    continue;
                }
                if (postIndex >= 0) {
                    installPostGuard(method, postIndex);
                    continue;
                }
                if (index < 0) continue;
                boolean item = names[index].endsWith(".MsgNotifyItem");
                try {
                    hook(method).intercept(chain -> {
                        int chatType = readChatType(chain.getArg(index), item);
                        boolean block = chatType == ConversationClassifier.GROUP
                                && !WhitelistConfig.shouldShowSource(hostContext, chain.getArg(index), chatType);
                        reportDecision("nt-" + (item ? "item" : "recent"), chatType, block);
                        return block ? null : chain.proceed();
                    });
                    ntMethods.add(method);
                    log(Log.INFO, TAG, "NT hook " + method.getName() + " kind="
                            + (item ? "item" : "recent") + " args=" + types.length);
                } catch (RuntimeException | LinkageError error) {
                    log(Log.WARN, TAG, "NT hook failed: " + error.getClass().getSimpleName());
                }
            }
            log(Log.INFO, TAG, "NT hooks=" + ntMethods.size() + " stage=" + stage + " process=" + processName);
        } catch (ClassNotFoundException | RuntimeException | LinkageError error) {
            log(Log.WARN, TAG, "NT unavailable stage=" + stage + " error=" + error.getClass().getSimpleName());
        }
    }

    private int readChatType(Object source, boolean item) {
        try {
            return NtChatTypeReader.read(source, item);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            reportReadFailure(error);
            return 0;
        }
    }

    private void installBuilderContext(Method method, int index, String typeName) {
        boolean item = QqNtSignatures.MSG_NOTIFY_ITEM.equals(typeName);
        try {
            hook(method).intercept(chain -> {
                int chatType = readChatType(chain.getArg(index), item);
                Set<String> ids = new HashSet<>();
                WhitelistConfig.collectObjectIds(chain.getArg(index), ids);
                // The nonvoid result is consumed by QQ; filter only its nested publication calls.
                return notificationTrace.during(chatType, ids, chain::proceed);
            });
            ntMethods.add(method);
            log(Log.INFO, TAG, "NT builder context=" + method.getName() + " kind=" + (item ? "item" : "recent"));
        } catch (RuntimeException | LinkageError error) {
            log(Log.WARN, TAG, "NT builder hook failed: " + error.getClass().getSimpleName());
        }
    }

    private void installPostGuard(Method method, int index) {
        try {
            hook(method).intercept(chain -> {
                Notification notification = (Notification) chain.getArg(index);
                notificationTrace.remember(notification);
                return interceptPost(notification, "nt-post", chain::proceed);
            });
            ntMethods.add(method);
            log(Log.INFO, TAG, "NT post guard=" + method.getName());
        } catch (RuntimeException | LinkageError error) {
            log(Log.WARN, TAG, "NT post hook failed: " + error.getClass().getSimpleName());
        }
    }

    private Object interceptPost(Notification notification, String source, NotificationTrace.Work original)
            throws Throwable {
        int classification = 0;
        boolean exempt = notification == null || Notification.CATEGORY_CALL.equals(notification.category)
                || (notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0;
        try {
            if (!exempt) classification = classifyNotification(notification);
        } catch (RuntimeException | LinkageError error) {
            reportReadFailure(error);
        }
        boolean configured = WhitelistConfig.isConfigured(hostContext);
        boolean blocked = !exempt && ConversationClassifier.shouldBlock(classification);
        if (!exempt && configured) blocked = !WhitelistConfig.shouldShow(hostContext, notification, classification,
                notificationTrace.idsFor(notification));
        reportDecision(source, classification, blocked);
        if (!exempt && !blocked && (classification == 0 || classification == 3)) {
            reportUnknownPost(source, classification);
        }
        return blocked ? null : original.run();
    }

    private void reportUnknownPost(String source, int classification) {
        int count = unknownPosts.incrementAndGet();
        if (count > 30 && count % 100 != 0) return;
        StringBuilder frames = new StringBuilder();
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (!frame.getClassName().startsWith("com.tencent.")) continue;
            if (frames.length() > 0) frames.append(" <- ");
            frames.append(frame.getClassName()).append('.').append(frame.getMethodName());
            if (frames.length() >= 1400) break;
        }
        log(Log.INFO, TAG, "unclassified-post source=" + source + " classification=" + classification
                + " process=" + processName + " count=" + count + " caller=" + frames);
    }

    private void reportReadFailure(Throwable error) {
        if (readFailures.incrementAndGet() <= 20) {
            log(Log.WARN, TAG, "Type read failed: " + error.getClass().getSimpleName());
        }
    }

    private void reportDecision(String source, int type, boolean blocked) {
        int count = decisions.incrementAndGet();
        if (count <= 100 || count % 100 == 0) {
            log(Log.INFO, TAG, "decision source=" + source + " type=" + type + " blocked=" + blocked
                    + " event=" + count + " process=" + processName);
        }
    }

    private void installSystemHooks() {
        if (installed) return;
        installed = true;
        captureConversationIntents();
        int count = 0;
        for (Method method : NotificationManager.class.getDeclaredMethods()) {
            String name = method.getName();
            if (!(name.equals("notify") || name.equals("notifyAsUser")
                    || name.equals("notifyAsPackage")) || method.getReturnType() != void.class) continue;
            int notificationIndex = indexOf(method.getParameterTypes(), Notification.class);
            if (notificationIndex < 0) continue;
            try {
                hook(method).intercept(chain -> {
                    Notification notification = (Notification) chain.getArg(notificationIndex);
                    return interceptPost(notification, "android-notify", chain::proceed);
                });
                count++;
            } catch (RuntimeException | LinkageError error) {
                log(Log.WARN, TAG, "Notification hook unavailable: " + method.getName());
            }
        }
        log(Log.INFO, TAG, "Android notification hooks=" + count);
    }

    private void captureConversationIntents() {
        for (Method method : PendingIntent.class.getDeclaredMethods()) {
            String name = method.getName();
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != PendingIntent.class
                    || !(name.equals("getActivity") || name.equals("getActivities")
                    || name.equals("getBroadcast") || name.equals("getService")
                    || name.equals("getForegroundService"))) continue;
            Class<?>[] parameters = method.getParameterTypes();
            int single = indexOf(parameters, Intent.class);
            int multiple = indexOf(parameters, Intent[].class);
            int intentIndex = Math.max(single, multiple);
            if (intentIndex < 0 || intentIndex + 1 >= parameters.length
                    || parameters[intentIndex + 1] != int.class) continue;
            try {
                hook(method).intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof PendingIntent)) return result;
                    try {
                        Object value = chain.getArg(intentIndex);
                        int classification = 0;
                        if (value instanceof Intent) {
                            classification = classify(((Intent) value).getExtras(), 0);
                        } else if (value instanceof Intent[]) {
                            for (Intent intent : (Intent[]) value) {
                                if (intent != null) classification |= classify(intent.getExtras(), 0);
                            }
                        }
                        int flags = (Integer) chain.getArg(intentIndex + 1);
                        if ((flags & PendingIntent.FLAG_NO_CREATE) == 0) {
                            synchronized (conversations) {
                                PendingIntent key = (PendingIntent) result;
                                if ((flags & PendingIntent.FLAG_UPDATE_CURRENT) != 0
                                        || !conversations.containsKey(key)) {
                                    conversations.put(key, classification);
                                }
                            }
                        }
                    } catch (RuntimeException | LinkageError ignored) {
                        // NotificationManager will still check the notification's own metadata.
                    }
                    return result;
                });
            } catch (RuntimeException | LinkageError error) {
                log(Log.WARN, TAG, "PendingIntent hook unavailable: " + method.getName());
            }
        }
    }

    private int classifyNotification(Notification notification) {
        int known = NotificationTrace.classification(notificationTrace.typeFor(notification));
        if (known != 0) return known;
        int classification = ConversationClassifier.classifyNotification(notification);
        if (classification == 0 && notification.extras != null) {
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            if (title != null && title.length() > 0) classification = ConversationClassifier.GROUP;
        }
        synchronized (conversations) {
            Integer intentType = conversations.get(notification.contentIntent);
            if (intentType != null) classification |= intentType;
        }
        return classification;
    }

    @SuppressWarnings("deprecation")
    private static int classify(Bundle bundle, int depth) {
        if (bundle == null || depth > 2) return 0;
        Map<String, Object> fields = new HashMap<>();
        for (String key : ConversationClassifier.KEYS) {
            if (bundle.containsKey(key)) fields.put(key, bundle.get(key));
        }
        int classification = ConversationClassifier.classify(fields);
        for (String key : new String[]{"intent", "extras", "extra", "data", "param",
                "params", "contact", "session", "bundle"}) {
            Object nested = bundle.get(key);
            if (nested instanceof Bundle) classification |= classify((Bundle) nested, depth + 1);
            else if (nested instanceof Intent) classification |= classify(((Intent) nested).getExtras(), depth + 1);
        }
        return classification;
    }

    private static int indexOf(Class<?>[] types, Class<?> expected) {
        for (int i = 0; i < types.length; i++) if (types[i] == expected) return i;
        return -1;
    }
}













