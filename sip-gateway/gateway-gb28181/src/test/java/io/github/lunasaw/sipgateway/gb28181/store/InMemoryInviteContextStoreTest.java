package io.github.lunasaw.sipgateway.gb28181.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryInviteContextStoreTest {

    @Test
    void testSaveAndFind() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        InviteContext ctx = new InviteContext("node-1", "ctx-key-1");
        store.save("call-id-1", ctx, 30_000);

        InviteContext found = store.find("call-id-1");
        assertNotNull(found);
        assertEquals("node-1", found.nodeId());
        assertEquals("ctx-key-1", found.ctxKey());
    }

    @Test
    void testFindMiss() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        assertNull(store.find("non-existent"));
    }

    @Test
    void testRemove() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        store.save("call-id-1", new InviteContext("node-1", "ctx-1"), 30_000);
        assertNotNull(store.find("call-id-1"));

        store.remove("call-id-1");
        assertNull(store.find("call-id-1"));
    }

    @Test
    void testOverwrite() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        store.save("call-id-1", new InviteContext("node-1", "ctx-1"), 30_000);
        store.save("call-id-1", new InviteContext("node-2", "ctx-2"), 30_000);

        InviteContext found = store.find("call-id-1");
        assertEquals("node-2", found.nodeId());
    }
}
