package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Layer 1 协议事件：REGISTER 收到 401 鉴权挑战。
 */
@Getter
public class ClientRegisterChallengeEvent extends ApplicationEvent {

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 鉴权挑战对应的 Call-ID。 */
    private final String callId;

    /**
     * 构造注册鉴权挑战事件。
     *
     * @param source 事件来源对象
     * @param userId 本端设备编码
     * @param callId 鉴权挑战对应的 Call-ID
     */
    public ClientRegisterChallengeEvent(Object source, String userId, String callId) {
        super(source);
        this.userId = userId;
        this.callId = callId;
    }
}
