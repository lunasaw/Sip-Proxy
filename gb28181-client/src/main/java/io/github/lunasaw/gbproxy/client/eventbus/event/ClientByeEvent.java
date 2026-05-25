package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientByeEvent extends ApplicationEvent {

    private final String callId;
    private final int statusCode;

    public ClientByeEvent(Object source, String callId, int statusCode) {
        super(source);
        this.callId = callId;
        this.statusCode = statusCode;
    }
}
