package com.example.qqprivatenotify;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationClassifierTest {
    @Test public void legacyAndNtNumericTypesAreNotInterchangeable() {
        assertTrue(blocks(Map.of("uintype", 1)));
        assertFalse(blocks(Map.of("uintype", 0)));
        assertFalse(blocks(Map.of("chatType", 1)));
        assertTrue(blocks(Map.of("chatType", 2)));
    }

    @Test public void privateTextAndTitleCannotTriggerFiltering() {
        assertFalse(blocks(Map.of("android.title", "[\u7fa4]", "android.text", "\u7fa4\u804a")));
        assertFalse(blocks(Map.of("chatType", 1, "android.text", "\u7fa4\u6d88\u606f")));
    }

    @Test public void unknownAndConflictingMetadataIsAllowed() {
        assertFalse(blocks(Map.of()));
        assertFalse(blocks(Map.of("chatType", 99)));
        assertFalse(blocks(Map.of("uintype", 0, "isGroup", true)));
        assertFalse(blocks(Map.of("uintype", 1, "chatType", 1)));
        assertFalse(blocks(Map.of("isGroup", "sometimes")));
        assertFalse(blocks(Map.of("conversation_type", 1)));
    }

    @Test public void standardMessagingStyleFlagIsRecognized() {
        assertTrue(blocks(Map.of("android.isGroupConversation", true)));
        assertFalse(blocks(Map.of("android.isGroupConversation", false)));
    }

    @Test public void androidDefaultFalseDoesNotOverrideQqGroupMetadata() {
        assertTrue(blocks(Map.of("android.isGroupConversation", false, "chatType", 2)));
        assertTrue(blocks(Map.of("android.isGroupConversation", false, "uintype", 1)));
        assertEquals(0, ConversationClassifier.classify(Map.of("android.isGroupConversation", false)));
    }

    @Test public void temporaryPrivateConversationsAreKept() {
        assertFalse(blocks(Map.of("chatType", 100)));
        assertFalse(blocks(Map.of("chatType", 100, "isGroup", true)));
    }

    @Test public void scalarEncodingDoesNotChangeConversationType() {
        assertTrue(blocks(Map.of("uin_type", "1")));
        assertTrue(blocks(Map.of("chat_type", 2L)));
        assertFalse(blocks(Map.of("chat_type", "1")));
        assertFalse(blocks(Map.of("chatType", 2.5)));
    }

    private boolean blocks(Map<String, ?> fields) {
        return ConversationClassifier.shouldBlock(ConversationClassifier.classify(fields));
    }
}
