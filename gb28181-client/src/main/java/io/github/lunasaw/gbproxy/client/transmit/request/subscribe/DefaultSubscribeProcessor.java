package io.github.lunasaw.gbproxy.client.transmit.request.subscribe;

import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;

/**
 * @author luna
 * @date 2023/12/29
 */
public class DefaultSubscribeProcessor implements SubscribeRequestHandler {
    @Override
    public void putSubscribe(String userId, SubscribeInfo subscribeInfo) {

    }

    @Override
    public DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery) {
        return null;
    }

}
