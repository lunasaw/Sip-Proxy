package io.github.lunasaw.gbproxy.server.transimit.response.subscribe;

import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import javax.sip.ResponseEvent;

/**
 * SUBSCRIBE响应处理器业务接口
 *
 * @author luna
 */
public interface SubscribeResponseProcessorServer {

    /**
     * 处理订阅成功响应
     *
     * @param deviceSubscribe 设备订阅信息
     */
    default void responseSubscribe(DeviceSubscribe deviceSubscribe) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理订阅失败响应
     *
     * @param evt 响应事件
     * @param callId 呼叫ID
     * @param statusCode 状态码
     */
    default void handleSubscribeFailure(ResponseEvent evt, String callId, int statusCode) {
        // 默认实现为空，由业务方根据需要实现
    }
}
