package io.github.lunasaw.gbproxy.gateway.store;

import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore.InviteContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试：内存版 InviteContextStore 的 save / find / remove 语义。
 */
class InMemoryInviteContextStoreTest {

    @Test
    void save_then_find_returns_pair() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);

        store.save("call-1", "node-A", "call-1_tag_1", 30_000);

        InviteContext ctx = store.find("call-1");
        assertThat(ctx).isNotNull();
        assertThat(ctx.nodeId()).isEqualTo("node-A");
        assertThat(ctx.ctxKey()).isEqualTo("call-1_tag_1");
    }

    @Test
    void find_unknown_callId_returns_null() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        assertThat(store.find("missing")).isNull();
    }

    @Test
    void remove_then_find_returns_null() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        store.save("call-1", "node-A", "ctx-1", 30_000);

        store.remove("call-1");

        assertThat(store.find("call-1")).isNull();
    }

    @Test
    void overwrite_keeps_latest_pair() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        store.save("call-1", "node-A", "ctx-1", 30_000);
        store.save("call-1", "node-B", "ctx-2", 30_000);

        InviteContext ctx = store.find("call-1");
        assertThat(ctx).isNotNull();
        assertThat(ctx.nodeId()).isEqualTo("node-B");
        assertThat(ctx.ctxKey()).isEqualTo("ctx-2");
    }

    @Test
    void store_isolates_different_callIds() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        store.save("call-A", "node-A", "ctx-A", 30_000);
        store.save("call-B", "node-B", "ctx-B", 30_000);

        InviteContext ctxA = store.find("call-A");
        InviteContext ctxB = store.find("call-B");

        assertThat(ctxA.nodeId()).isEqualTo("node-A");
        assertThat(ctxB.nodeId()).isEqualTo("node-B");
        assertThat(ctxA.ctxKey()).isEqualTo("ctx-A");
        assertThat(ctxB.ctxKey()).isEqualTo("ctx-B");
    }

    @Test
    void implements_InviteContextStore_interface() {
        InMemoryInviteContextStore store = new InMemoryInviteContextStore(30_000);
        assertThat(store).isInstanceOf(InviteContextStore.class);
    }
}
