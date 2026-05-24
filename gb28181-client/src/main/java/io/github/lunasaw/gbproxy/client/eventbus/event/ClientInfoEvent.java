package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 客户端收到 INFO 请求事件。处理器已自动回 200 OK。
 */
@Getter
public class ClientInfoEvent extends ApplicationEvent {

    private final String userId;
    private final String content;

    public ClientInfoEvent(Object source, String userId, String content) {
        super(source);
        this.userId = userId;
        this.content = content;
    }
}
