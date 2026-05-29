package io.github.lunasaw.gbproxy.server.api.dto;

/**
 * 设备生命周期事件子类型，用于 {@code ServerLifecycleEvent} 区分语义。
 *
 * @author luna
 */
public enum LifecycleType {
    /** 设备首次注册成功 */
    REGISTER,
    /** 注册挑战（digest auth） */
    CHALLENGE,
    /** 设备进入在线状态 */
    ONLINE,
    /** 设备离线 */
    OFFLINE,
    /** 设备远端地址变化（NAT 漂移） */
    REMOTE_ADDRESS_CHANGED
}
