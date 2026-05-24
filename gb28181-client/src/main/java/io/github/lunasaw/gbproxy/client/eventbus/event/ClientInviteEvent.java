package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientInviteEvent extends ApplicationEvent {

    private final String callId;
    private final String userId;
    private final SdpSessionDescription sessionDescription;
    private final String transactionContextKey;

    public ClientInviteEvent(Object source, String callId, String userId, SdpSessionDescription sessionDescription, String transactionContextKey) {
        super(source);
        this.callId = callId;
        this.userId = userId;
        this.sessionDescription = sessionDescription;
        this.transactionContextKey = transactionContextKey;
    }
}
