package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceMobileQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;

/**
 * 平台订阅监听器（method=SUBSCRIBE，client 角色业务方实现）。
 *
 * <p>fire-and-forget 通知语义：listener 不能拒绝订阅。SIP 事务的 200 OK 由 L0 handler
 * 同步返回（毫秒级），listener 仅接收"已接受订阅"通知。
 *
 * <p>真要做接受/拒绝决策（业务认证、限流），可在 Layer 1 协议事件层（{@code ClientSubscribeEvent}）
 * 通过 BeanPostProcessor / 全局拦截器实现，不下放到 listener。
 *
 * @author luna
 */
public interface SubscribeListener {

    /** 平台目录订阅（cmdType=Catalog，body=DeviceQuery）。 */
    default void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery query) {}

    /** 平台报警订阅（cmdType=Alarm，body=DeviceAlarmQuery）。 */
    default void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery query) {}

    /** 平台移动位置订阅（cmdType=MobilePosition，body=DeviceMobileQuery）。 */
    default void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery query) {}
}
