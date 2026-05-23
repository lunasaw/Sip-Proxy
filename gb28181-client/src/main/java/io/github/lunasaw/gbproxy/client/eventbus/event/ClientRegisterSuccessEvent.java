package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientRegisterSuccessEvent extends ApplicationEvent {

    private final String userId;

    public ClientRegisterSuccessEvent(Object source, String userId) {
        super(source);
        this.userId = userId;
    }
}
