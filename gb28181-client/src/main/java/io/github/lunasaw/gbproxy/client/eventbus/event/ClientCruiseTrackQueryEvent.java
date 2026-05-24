package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackQuery;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.5 / A.2.4.12 巡航轨迹查询请求事件（客户端收到平台查询）
 *
 * @author luna
 */
@Getter
public class ClientCruiseTrackQueryEvent extends ApplicationEvent {

    private final String userId;
    private final String sipId;
    private final CruiseTrackQuery query;

    public ClientCruiseTrackQueryEvent(Object source, String userId, String sipId, CruiseTrackQuery query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.query = query;
    }
}
