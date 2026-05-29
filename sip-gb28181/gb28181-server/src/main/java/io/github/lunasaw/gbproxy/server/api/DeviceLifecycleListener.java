package io.github.lunasaw.gbproxy.server.api;

import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;

/**
 * 设备生命周期监听器（server 角色业务方实现）。
 *
 * <p>覆盖：注册（首次/挑战）/ 在线 / 离线（含原因）/ 远端地址变更。fire-and-forget。
 *
 * @author luna
 */
public interface DeviceLifecycleListener {

    /** 设备首次注册成功（含 RegisterInfo）。 */
    default void onDeviceRegister(String deviceId, RegisterInfo registerInfo) {}

    /** 注册挑战（digest auth），业务方可观察认证流程。 */
    default void onRegisterChallenge(String deviceId) {}

    /** 设备进入在线状态（已通过注册 + keepalive 验活）。 */
    default void onDeviceOnline(String deviceId, SipTransaction sipTransaction) {}

    /** 设备离线（超时 / 主动注销 / 重新注册）。 */
    default void onDeviceOffline(String deviceId, RegisterInfo registerInfo, SipTransaction sipTransaction) {}

    /** 设备远端地址变化（IP/端口漂移，常见于 NAT 重连）。 */
    default void onRemoteAddressChanged(String deviceId, RemoteAddressInfo remoteAddressInfo) {}
}
