package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 协议层内部订阅注册表（v1.5.0 新增）。
 *
 * <p>v1.5.0 改造：原 {@code SubscribeRequestHandler.putSubscribe(userId, subscribeInfo)} 由业务方实现，
 * 现在改由 SUBSCRIBE 处理器在发出 200 OK 时直接调 {@link #put(String, SubscribeInfo)} 维护。
 * 业务方无需感知此注册表。
 *
 * <p>设计取舍：单进程内的内存表足够支撑大部分场景；多节点部署需要把 SubscribeInfo 外置到 Redis
 * （那是 v2.0+ 议题，与 DeviceSessionCache 同形态）。
 *
 * @author luna
 */
public final class SubscribeRegistry {

    private static final ConcurrentMap<String, SubscribeInfo> SUBSCRIBES = new ConcurrentHashMap<>();

    private SubscribeRegistry() {}

    /**
     * 注册或更新设备的订阅信��。
     *
     * @param deviceId 设备 ID
     * @param info     订阅元信息（含 expires / callId / fromTag 等）
     */
    public static void put(String deviceId, SubscribeInfo info) {
        if (deviceId == null || info == null) {
            return;
        }
        SUBSCRIBES.put(deviceId, info);
    }

    /**
     * 取出指定设备的订阅信息（业务方主动催发 NOTIFY 时使用）。
     *
     * @param deviceId 设备 ID
     * @return 订阅信息，不存在时返回 null
     */
    public static SubscribeInfo get(String deviceId) {
        return deviceId == null ? null : SUBSCRIBES.get(deviceId);
    }

    /**
     * 注销设备订阅（订阅过期或显式 UNSUBSCRIBE 时使用）。
     */
    public static SubscribeInfo remove(String deviceId) {
        return deviceId == null ? null : SUBSCRIBES.remove(deviceId);
    }

    /** 仅供单元测试使用，清空注册表。 */
    static void clear() {
        SUBSCRIBES.clear();
    }
}
