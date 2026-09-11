package com.example.qqprivatenotify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NotificationTraceTest {
    @Test public void preservesOriginalResultAndClearsCompletedScope() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Object result = new Object();

        assertSame(result, trace.during(2, () -> result));
        assertEquals(0, trace.typeFor(new Object()));
    }

    @Test public void propagatesOriginalFailureAndClearsFailedScope() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Exception failure = new Exception("host failure");

        try {
            trace.during(2, () -> { throw failure; });
            fail("Expected the original host failure");
        } catch (Exception actual) {
            assertSame(failure, actual);
        }
        assertEquals(0, trace.typeFor(new Object()));
    }

    @Test public void nestedPrivateAndUnknownNotificationsShadowOuterGroup() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Object notification = new Object();

        trace.during(2, () -> {
            assertEquals(2, trace.typeFor(notification));
            trace.during(1, () -> {
                assertEquals(1, trace.typeFor(notification));
                return null;
            });
            assertEquals(2, trace.typeFor(notification));
            trace.during(0, () -> {
                assertEquals(0, trace.typeFor(notification));
                return null;
            });
            assertEquals(2, trace.typeFor(notification));
            return null;
        });

        assertEquals(0, trace.typeFor(notification));
    }

    @Test public void failedNestedScopeRestoresOuterNotificationType() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Exception failure = new Exception("nested host failure");

        trace.during(2, () -> {
            try {
                trace.during(1, () -> { throw failure; });
                fail("Expected the original nested host failure");
            } catch (Exception actual) {
                assertSame(failure, actual);
            }
            assertEquals(2, trace.typeFor(new Object()));
            return null;
        });

        assertEquals(0, trace.typeFor(new Object()));
    }

    @Test public void simultaneousGroupAndPrivateNotificationsRemainIndependent() throws Exception {
        NotificationTrace trace = new NotificationTrace();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try {
            Future<?> group = workers.submit(() -> checkConcurrentScope(trace, 2, entered, release));
            Future<?> direct = workers.submit(() -> checkConcurrentScope(trace, 1, entered, release));
            assertTrue("Both notification scopes must be active", entered.await(5, TimeUnit.SECONDS));
            assertEquals(0, trace.typeFor(new Object()));
            release.countDown();
            group.get(5, TimeUnit.SECONDS);
            direct.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test public void rememberedNotificationRetainsItsTypeAfterHandoff() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Object notification = new Object();
        trace.during(2, () -> {
            trace.remember(notification);
            return null;
        });

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> result = worker.submit(() -> trace.typeFor(notification));
            assertEquals(2, result.get(5, TimeUnit.SECONDS).intValue());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test public void rememberedGroupDoesNotClassifyAnotherNotification() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Object notification = new Object();
        trace.during(2, () -> {
            trace.remember(notification);
            return null;
        });

        assertEquals(2, trace.typeFor(notification));
        assertEquals(0, trace.typeFor(new Object()));
        assertEquals(0, trace.typeFor(null));
    }

    @Test public void rememberingUnknownReuseReplacesEarlierGroupClassification() throws Throwable {
        NotificationTrace trace = new NotificationTrace();
        Object notification = new Object();

        trace.during(2, () -> {
            trace.remember(notification);
            trace.during(0, () -> {
                trace.remember(notification);
                assertEquals(0, trace.typeFor(notification));
                return null;
            });
            assertEquals(2, trace.typeFor(notification));
            return null;
        });

        assertEquals(0, trace.typeFor(notification));
    }

    @Test public void knownHostChatTypesKeepPrivateAndGroupSemantics() {
        assertEquals(ConversationClassifier.GROUP, NotificationTrace.classification(2));
        assertEquals(ConversationClassifier.PRIVATE, NotificationTrace.classification(1));
        assertEquals(ConversationClassifier.PRIVATE, NotificationTrace.classification(100));
        for (int unknown : new int[]{0, -1, 3, 4, 101, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertEquals(0, NotificationTrace.classification(unknown));
        }
    }

    private static void checkConcurrentScope(NotificationTrace trace, int type,
            CountDownLatch entered, CountDownLatch release) {
        try {
            trace.during(type, () -> {
                entered.countDown();
                assertTrue("Notification scopes must be released", release.await(5, TimeUnit.SECONDS));
                assertEquals(type, trace.typeFor(new Object()));
                return null;
            });
            assertEquals(0, trace.typeFor(new Object()));
        } catch (Throwable error) {
            throw new AssertionError("Concurrent notification scope failed", error);
        }
    }
}
