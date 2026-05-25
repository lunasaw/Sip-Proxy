package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientRegisterFailureEvent extends ApplicationEvent {

    private final String userId;
    private final int statusCode;

    public ClientRegisterFailureEvent(Object source, String userId, int statusCode) {
        super(source);
        this.userId = userId;
        this.statusCode = statusCode;
    }
}
