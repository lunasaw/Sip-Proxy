package io.github.lunasaw.gbproxy.client.eventbus.event;

import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.11.1 收到平台对报警事件的 SUBSCRIBE 请求事件。
 *
 * 业务方在 {@code @EventListener} 中维护订阅会话状态（接受/续订/拒绝）。
 *
 * @author luna
 */
@Getter
public class ClientAlarmSubscribeEvent extends ApplicationEvent {

    private final String userId;
    private final String sipId;
    private final Integer expires;
    private final DeviceAlarmQuery query;

    public ClientAlarmSubscribeEvent(Object source, String userId, String sipId, Integer expires, DeviceAlarmQuery query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.expires = expires;
        this.query = query;
    }
}
