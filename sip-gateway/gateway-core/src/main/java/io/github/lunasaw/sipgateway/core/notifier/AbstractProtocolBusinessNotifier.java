package io.github.lunasaw.sipgateway.core.notifier;

import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;

/**
 * 可选基类：按 protocol 分发。
 *
 * @author luna
 */
public abstract class AbstractProtocolBusinessNotifier implements BusinessNotifier {

    @Override
    public final void notify(GatewayEvent event) {
        String type = event.type();
        int firstDot = type.indexOf('.');
        String protocol = firstDot > 0 ? type.substring(0, firstDot) : "unknown";
        onProtocolEvent(protocol, event);
    }

    /**
     * 子类按 protocol 分支或拆方法。
     */
    protected abstract void onProtocolEvent(String protocol, GatewayEvent event);
}
