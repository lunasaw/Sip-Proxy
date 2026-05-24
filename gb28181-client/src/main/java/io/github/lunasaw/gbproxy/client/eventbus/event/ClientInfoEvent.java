package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 客户端收到 INFO 请求事件。处理器已自动回 200 OK。
 *
 * 当 Content-Type 为 Application/MANSRTSP 时，{@link #parsed} 会携带结构化的
 * {@link ManSrtspRequest}（GB28181-2022 附录 B）。其他 Content-Type 时该字段为 null。
 */
@Getter
public class ClientInfoEvent extends ApplicationEvent {

    private final String userId;
    private final String content;
    private final String contentType;
    private final ManSrtspRequest parsed;

    public ClientInfoEvent(Object source, String userId, String content) {
        this(source, userId, content, null, null);
    }

    public ClientInfoEvent(Object source, String userId, String content, String contentType, ManSrtspRequest parsed) {
        super(source);
        this.userId = userId;
        this.content = content;
        this.contentType = contentType;
        this.parsed = parsed;
    }
}

