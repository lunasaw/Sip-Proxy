package io.github.lunasaw.sipgateway.core.api;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;

/**
 * 业务推送接口（业务方实现）。
 *
 * @author luna
 */
public interface BusinessNotifier {
    /**
     * 业务方实现：把 event 推到 HTTP/MQ/Webhook。
     * <p><strong>必须异步</strong>，否则会阻塞 SIP 事件线程导致设备超时重传。
     */
    void notify(GatewayEvent event);
}
