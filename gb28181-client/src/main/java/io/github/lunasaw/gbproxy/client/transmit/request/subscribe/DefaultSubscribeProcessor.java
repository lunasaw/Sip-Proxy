package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认的 SubscribeRequestHandler 实现
 * 提供基本的空实现，业务项目可以自定义实现来覆盖此默认行为
 * 
 * @author luna
 * @date 2023/12/29
 */
@Component
@ConditionalOnMissingBean(name = "io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestHandler")
public class DefaultSubscribeProcessor implements SubscribeRequestHandler {
    @Override
    public void putSubscribe(String userId, SubscribeInfo subscribeInfo) {
        // 默认实现为空，业务项目可以覆盖
    }

    @Override
    public DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery) {
        // 默认实现返回null，业务项目可以覆盖
        return null;
    }
}
