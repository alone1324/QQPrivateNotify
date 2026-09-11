package com.example.qqprivatenotify;

import org.junit.Test;
import static org.junit.Assert.*;

public class NtChatTypeReaderTest {
    static class Recent { private final int chatType; Recent(int value) { chatType = value; } }
    static class Child extends Recent { Child(int value) { super(value); } }
    static class Item { private final Recent msgInfo; Item(Recent info) { msgInfo = info; } }
    static class GetterRecent { public int getChatType() { return 2; } }
    static class GetterItem { public GetterRecent getMsgInfo() { return new GetterRecent(); } }
    static class Unknown { String chatType = "2"; }
    static class BrokenGetter { public int getChatType() { throw new IllegalStateException(); } }

    @Test public void recentInfoReadsGroupAndPrivateAndTemporaryTypes() throws Exception {
        assertEquals(2, NtChatTypeReader.read(new Recent(2), false));
        assertEquals(1, NtChatTypeReader.read(new Recent(1), false));
        assertEquals(100, NtChatTypeReader.read(new Recent(100), false));
    }

    @Test public void notifyItemReadsNestedType() throws Exception {
        assertEquals(2, NtChatTypeReader.read(new Item(new Recent(2)), true));
        assertEquals(1, NtChatTypeReader.read(new Item(new Recent(1)), true));
    }

    @Test public void inheritedFieldsAndGetterOnlyModelsWork() throws Exception {
        assertEquals(2, NtChatTypeReader.read(new Child(2), false));
        assertEquals(2, NtChatTypeReader.read(new GetterRecent(), false));
        assertEquals(2, NtChatTypeReader.read(new GetterItem(), true));
    }

    @Test public void nullOrUnknownTypeIsNotInvented() throws Exception {
        assertEquals(0, NtChatTypeReader.read(null, false));
        assertEquals(0, NtChatTypeReader.read(new Item(null), true));
        assertEquals(0, NtChatTypeReader.read(new Unknown(), false));
        assertEquals(999, NtChatTypeReader.read(new Recent(999), false));
    }

    @Test public void missingFieldOrThrowingGetterIsReportedToCaller() {
        assertThrows(ReflectiveOperationException.class, () -> NtChatTypeReader.read(new Object(), false));
        assertThrows(ReflectiveOperationException.class, () -> NtChatTypeReader.read(new BrokenGetter(), false));
    }
}
