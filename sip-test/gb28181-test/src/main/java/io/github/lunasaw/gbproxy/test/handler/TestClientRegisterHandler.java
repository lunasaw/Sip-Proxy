package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class TestClientRegisterHandler {

    private final AtomicBoolean registered = new AtomicBoolean(false);
    private volatile CountDownLatch latch;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        this.registered.set(false);
    }

    public boolean isRegistered() {
        return registered.get();
    }

    @EventListener
    public void onRegisterSuccess(ClientRegisterSuccessEvent event) {
        registered.set(true);
        log.info("注册成功: userId={}", event.getUserId());
        if (latch != null) {
            latch.countDown();
        }
    }

    @EventListener
    public void onRegisterFailure(ClientRegisterFailureEvent event) {
        log.warn("注册失败: userId={}, statusCode={}", event.getUserId(), event.getStatusCode());
        if (latch != null) {
            latch.countDown();
        }
    }
}
