package com.example.qqprivatenotify;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Scoped host type plus the actual notification object, never a reusable notification ID. */
final class NotificationTrace {
    interface Work { Object run() throws Throwable; }
    private final ThreadLocal<Integer> currentType = new ThreadLocal<>();
    private final ThreadLocal<java.util.Set<String>> currentIds = new ThreadLocal<>();
    private final Map<Object, Integer> postedTypes = Collections.synchronizedMap(new WeakHashMap<>());

    Object during(int type, Work work) throws Throwable {
        return during(type, null, work);
    }

    Object during(int type, java.util.Set<String> ids, Work work) throws Throwable {
        Integer previous = currentType.get();
        java.util.Set<String> previousIds = currentIds.get();
        currentType.set(type);
        if (ids == null || ids.isEmpty()) currentIds.remove(); else currentIds.set(ids);
        try {
            return work.run();
        } finally {
            if (previous == null) currentType.remove();
            else currentType.set(previous);
            if (previousIds == null) currentIds.remove(); else currentIds.set(previousIds);
        }
    }

    int typeFor(Object notification) {
        Integer type = currentType.get();
        if (type != null) return type;
        if (notification == null) return 0;
        Integer saved = postedTypes.get(notification);
        return saved == null ? 0 : saved;
    }

    java.util.Set<String> idsFor(Object notification) {
        java.util.Set<String> ids = currentIds.get();
        return ids == null ? java.util.Collections.emptySet() : ids;
    }

    void remember(Object notification) {
        Integer type = currentType.get();
        if (notification != null && type != null) postedTypes.put(notification, type);
    }

    static int classification(int type) {
        if (type == 2) return ConversationClassifier.GROUP;
        if (type == 1 || type == 100) return ConversationClassifier.PRIVATE;
        return 0;
    }
}
