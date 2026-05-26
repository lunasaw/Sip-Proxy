package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent;
import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 集成测试用 L1 {@link ClientInfoEvent} 捕获 bean (供 InfoMansrtspFlowTest 使用)。
 *
 * <p>INFO 是 L1 直消费事件（无 L2 listener），FlowTest 通过 reset(latch)+typed getter 断言。
 *
 * @author luna
 */
@Component
@Getter
public class TestInfoEventCapture {

    private volatile CountDownLatch latch;
    private volatile String lastContent;
    private volatile String lastContentType;
    private volatile ManSrtspRequest lastParsed;

    public void reset(CountDownLatch latch) {
        this.latch = latch;
        this.lastContent = null;
        this.lastContentType = null;
        this.lastParsed = null;
    }

    @EventListener
    public void onInfo(ClientInfoEvent e) {
        this.lastContent = e.getContent();
        this.lastContentType = e.getContentType();
        this.lastParsed = e.getParsed();
        if (latch != null) latch.countDown();
    }
}
