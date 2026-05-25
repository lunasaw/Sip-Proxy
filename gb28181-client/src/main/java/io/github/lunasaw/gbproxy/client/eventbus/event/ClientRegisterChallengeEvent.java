package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientRegisterChallengeEvent extends ApplicationEvent {

    private final String userId;
    private final String callId;

    public ClientRegisterChallengeEvent(Object source, String userId, String callId) {
        super(source);
        this.userId = userId;
        this.callId = callId;
    }
}
