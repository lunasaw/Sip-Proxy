package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientAckEvent extends ApplicationEvent {

    private final String callId;

    public ClientAckEvent(Object source, String callId) {
        super(source);
        this.callId = callId;
    }
}
