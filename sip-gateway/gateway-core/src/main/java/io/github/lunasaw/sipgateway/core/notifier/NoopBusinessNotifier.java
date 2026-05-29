package io.github.lunasaw.sipgateway.core.notifier;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 BusinessNotifier 实现：仅日志输出，启动 warn。
 *
 * @author luna
 */
public class NoopBusinessNotifier implements BusinessNotifier {

    private static final Logger log = LoggerFactory.getLogger(NoopBusinessNotifier.class);

    private volatile boolean warned = false;

    @Override
    public void notify(GatewayEvent event) {
        if (!warned) {
            log.warn("NoopBusinessNotifier active — replace with production implementation before deployment");
            warned = true;
        }
        log.debug("GatewayEvent: type={}, deviceId={}, correlationId={}",
                event.type(), event.deviceId(), event.correlationId());
    }
}
