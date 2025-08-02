package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @date 2023/12/29
 */
@Component
@ConditionalOnMissingBean(SubscribeRequestHandler.class)
public class DefaultSubscribeProcessor implements SubscribeRequestHandler {
    @Override
    public void putSubscribe(String userId, SubscribeInfo subscribeInfo) {

    }

    @Override
    public DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery) {
        return null;
    }

}
