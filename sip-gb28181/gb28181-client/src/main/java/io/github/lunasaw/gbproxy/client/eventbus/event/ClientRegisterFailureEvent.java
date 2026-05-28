package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：REGISTER 注册失败（非 200/401 响应）。
 */
@Getter
public class ClientRegisterFailureEvent extends ApplicationEvent {

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** SIP 失败响应状态码。 */
    private final int statusCode;

    /**
     * 构造注册失败事件。
     *
     * @param source     事件来源对象
     * @param userId     本端设备编码
     * @param statusCode SIP 失败响应状态码
     */
    public ClientRegisterFailureEvent(Object source, String userId, int statusCode) {
        super(source);
        this.userId = userId;
        this.statusCode = statusCode;
    }
}
