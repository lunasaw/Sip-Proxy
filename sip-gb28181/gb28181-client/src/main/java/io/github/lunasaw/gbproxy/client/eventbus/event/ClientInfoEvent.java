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

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** INFO 消息体原始内容。 */
    private final String content;
    /** Content-Type 头字段值。 */
    private final String contentType;
    /** 当 Content-Type 为 Application/MANSRTSP 时的结构化解析结果，其他类型为 null。 */
    private final ManSrtspRequest parsed;

    /**
     * 构造 INFO 事件（不含 Content-Type 和结构化解析结果）。
     *
     * @param source  事件来源对象
     * @param userId  本端设备编码
     * @param content INFO 消息体原始内容
     */
    public ClientInfoEvent(Object source, String userId, String content) {
        this(source, userId, content, null, null);
    }

    /**
     * 构造 INFO 事件（含 Content-Type 和结构化解析结果）。
     *
     * @param source      事件来源对象
     * @param userId      本端设备编码
     * @param content     INFO 消息体原始内容
     * @param contentType Content-Type 头字段值
     * @param parsed      MANSRTSP 结构化解析结果，非 MANSRTSP 时为 null
     */
    public ClientInfoEvent(Object source, String userId, String content, String contentType, ManSrtspRequest parsed) {
        super(source);
        this.userId = userId;
        this.content = content;
        this.contentType = contentType;
        this.parsed = parsed;
    }
}

