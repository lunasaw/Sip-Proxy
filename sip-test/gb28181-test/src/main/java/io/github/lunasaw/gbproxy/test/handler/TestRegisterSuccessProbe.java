package io.github.lunasaw.gbproxy.test.handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听 {@link ClientRegisterSuccessEvent}，捕获对端 X-GB-Ver 协议版本。
 *
 * <p>用于 GBT-28181-2022 附录 I 端到端测试。
 */
@Component
public class TestRegisterSuccessProbe {

    private final AtomicReference<String> peerProtocolVersion = new AtomicReference<>();

    @Getter
    private volatile String userId;

    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        this.peerProtocolVersion.set(null);
        this.userId = null;
    }

    public String getPeerProtocolVersion() {
        return peerProtocolVersion.get();
    }

    @EventListener
    public void onSuccess(ClientRegisterSuccessEvent event) {
        this.peerProtocolVersion.set(event.getPeerProtocolVersion());
        this.userId = event.getUserId();
        if (latch != null) {
            latch.countDown();
        }
    }
}
