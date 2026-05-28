package io.github.lunasaw.gbproxy.server.transmit.event;

import io.github.lunasaw.gbproxy.server.api.dto.LifecycleType;
import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Server 端 Layer 1 协议事件：设备生命周期（注册/挑战/在线/离线/远端地址变更）。
 *
 * <p>用 {@link LifecycleType} 区分子语义，由 {@code ServerListenerAdapter} 分发到
 * {@code DeviceLifecycleListener} 的对应方法。
 *
 * @author luna
 */
@Getter
public class ServerLifecycleEvent extends ApplicationEvent {

    private final LifecycleType type;
    private final String deviceId;
    /** type=REGISTER 或 OFFLINE 时非空 */
    private final RegisterInfo registerInfo;
    /** type=ONLINE 或 OFFLINE 时非空 */
    private final SipTransaction sipTransaction;
    /** type=REMOTE_ADDRESS_CHANGED 时非空 */
    private final RemoteAddressInfo remoteAddressInfo;

    public ServerLifecycleEvent(Object source, LifecycleType type, String deviceId,
                                RegisterInfo registerInfo, SipTransaction sipTransaction,
                                RemoteAddressInfo remoteAddressInfo) {
        super(source);
        this.type = type;
        this.deviceId = deviceId;
        this.registerInfo = registerInfo;
        this.sipTransaction = sipTransaction;
        this.remoteAddressInfo = remoteAddressInfo;
    }

    public static ServerLifecycleEvent register(Object source, String deviceId, RegisterInfo info) {
        return new ServerLifecycleEvent(source, LifecycleType.REGISTER, deviceId, info, null, null);
    }

    public static ServerLifecycleEvent challenge(Object source, String deviceId) {
        return new ServerLifecycleEvent(source, LifecycleType.CHALLENGE, deviceId, null, null, null);
    }

    public static ServerLifecycleEvent online(Object source, String deviceId, SipTransaction tx) {
        return new ServerLifecycleEvent(source, LifecycleType.ONLINE, deviceId, null, tx, null);
    }

    public static ServerLifecycleEvent offline(Object source, String deviceId, RegisterInfo info, SipTransaction tx) {
        return new ServerLifecycleEvent(source, LifecycleType.OFFLINE, deviceId, info, tx, null);
    }

    public static ServerLifecycleEvent remoteAddressChanged(Object source, String deviceId, RemoteAddressInfo addr) {
        return new ServerLifecycleEvent(source, LifecycleType.REMOTE_ADDRESS_CHANGED, deviceId, null, null, addr);
    }
}
