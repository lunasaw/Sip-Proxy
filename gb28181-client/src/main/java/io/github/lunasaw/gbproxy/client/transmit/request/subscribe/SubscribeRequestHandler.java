package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;

/**
 * SUBSCRIBE请求业务处理器接口
 * 负责处理SUBSCRIBE请求的业务逻辑
 *
 * @author luna
 * @version 1.0
 * @date 2023/12/11
 */
public interface SubscribeRequestHandler {

    /**
     * 添加订阅信息
     *
     * @param userId        用户ID
     * @param subscribeInfo 订阅信息
     */
    void putSubscribe(String userId, SubscribeInfo subscribeInfo);

    /**
     * 获取设备订阅信息
     *
     * @param deviceQuery 设备查询
     * @return DeviceSubscribe 设备订阅信息
     */
    DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery);
}
