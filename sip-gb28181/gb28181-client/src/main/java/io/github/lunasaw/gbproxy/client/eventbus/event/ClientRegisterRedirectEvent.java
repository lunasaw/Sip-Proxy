package io.github.lunasaw.gbproxy.client.eventbus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * GB28181-2022 §9.1.2.3 / RFC 3261 §21.3.3 注册重定向事件。
 *
 * <p>client 收到 302 Moved Temporarily 时由 {@code ClientRegisterResponseProcessor} 发布。
 * 框架内部会自动按 Contact 头中的新地址再次发起 REGISTER；业务方可监听此事件做审计 / metrics。
 *
 * @author luna
 */
@Getter
public class ClientRegisterRedirectEvent extends ApplicationEvent {

    /** 本端设备编码（deviceId）。 */
    private final String userId;
    /** 重定向目标 SIP 服务器地址（来自 302 响应的 Contact 头：sip:user@host:port） */
    private final String redirectContact;
    /** 重定向目标 SIP 服务器编码（GB28181 ID） */
    private final String redirectUserId;
    /** 重定向目标 SIP 服务器主机：IP[:port] */
    private final String redirectHost;
    /** 重定向目标 SIP 服务器端口。 */
    private final Integer redirectPort;
    /** 302 响应中携带的新 Expires（秒），可能为 null */
    private final Integer expires;

    /**
     * 构造注册重定向事件。
     *
     * @param source          事件来源对象
     * @param userId          本端设备编码
     * @param redirectContact 重定向目标 Contact 地址
     * @param redirectUserId  重定向目标 GB28181 编码
     * @param redirectHost    重定向目标主机
     * @param redirectPort    重定向目标端口
     * @param expires         302 响应中的 Expires 值，可能为 null
     */
    public ClientRegisterRedirectEvent(Object source, String userId,
                                       String redirectContact, String redirectUserId,
                                       String redirectHost, Integer redirectPort,
                                       Integer expires) {
        super(source);
        this.userId = userId;
        this.redirectContact = redirectContact;
        this.redirectUserId = redirectUserId;
        this.redirectHost = redirectHost;
        this.redirectPort = redirectPort;
        this.expires = expires;
    }
}
