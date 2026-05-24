package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.query.HomePositionQuery;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.5 / A.2.4.10 看守位信息查询请求事件（客户端收到平台查询）
 *
 * @author luna
 */
@Getter
public class ClientHomePositionQueryEvent extends ApplicationEvent {

    private final String userId;
    private final String sipId;
    private final HomePositionQuery query;

    public ClientHomePositionQueryEvent(Object source, String userId, String sipId, HomePositionQuery query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.query = query;
    }
}
