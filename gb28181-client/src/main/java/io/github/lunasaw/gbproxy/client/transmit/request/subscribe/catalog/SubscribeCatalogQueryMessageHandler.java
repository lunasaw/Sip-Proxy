package io.github.lunasaw.gbproxy.client.transmit.request.subscribe.catalog;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestHandler;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;

/**
 * 处理设备通道订阅消息 回复OK
 *
 * @author luna
 * @date 2023/10/19
 */
@Component("subscribeCatalogQueryMessageHandler")
@Slf4j
@Getter
@Setter
public class SubscribeCatalogQueryMessageHandler extends SubscribeHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.CATALOG.getType();

    public SubscribeCatalogQueryMessageHandler(SubscribeRequestHandler subscribeRequestHandler, ClientDeviceSupplier deviceSupplier) {
        super(subscribeRequestHandler, deviceSupplier);
    }

    @Override
    public String getRootType() {
        return MessageHandler.QUERY;
    }

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceSession deviceSession = getDeviceSession(event);

        EventHeader header = (EventHeader) event.getRequest().getHeader(EventHeader.NAME);
        if (header == null) {
            log.info("handForEvt::event = {}", event);
            return;
        }

        // 订阅消息过来
        String sipId = deviceSession.getSipId();
        String userId = deviceSession.getUserId();
        SIPRequest request = (SIPRequest) event.getRequest();
        SubscribeInfo subscribeInfo = new SubscribeInfo(request, sipId);
        Device fromDevice = deviceSession.getFromDevice();
        if (!userId.equals(fromDevice.getUserId())) {
            return;
        }

        DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
        subscribeRequestHandler.putSubscribe(deviceQuery.getDeviceId(), subscribeInfo);

        DeviceSubscribe deviceSubscribe = subscribeRequestHandler.getDeviceSubscribe(deviceQuery);
        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());

        ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, deviceSubscribe.toString(), contentTypeHeader, event, expiresHeader);
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }

    @Override
    public boolean needResponseAck() {
        return false;
    }
}
