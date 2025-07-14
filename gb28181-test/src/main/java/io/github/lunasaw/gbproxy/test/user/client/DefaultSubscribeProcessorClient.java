package io.github.lunasaw.gbproxy.test.user.client;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeProcessorClient;
import io.github.lunasaw.sip.common.subscribe.SubscribeHolder;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/11
 * @description:
 */
@Component
public class DefaultSubscribeProcessorClient implements SubscribeProcessorClient {

    @Autowired
    private SubscribeHolder subscribeHolder;


    @Override
    public void putSubscribe(String userId, SubscribeInfo subscribeInfo) {
        subscribeHolder.putCatalogSubscribe(userId, subscribeInfo);
    }

    @Override
    public DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery) {
        DeviceSubscribe deviceSubscribe = new DeviceSubscribe();
        deviceSubscribe.setDeviceId(deviceQuery.getDeviceId());
        deviceSubscribe.setCmdType(CmdTypeEnum.CATALOG.getType());
        deviceSubscribe.setSn(deviceQuery.getSn());
        return deviceSubscribe;
    }
}
