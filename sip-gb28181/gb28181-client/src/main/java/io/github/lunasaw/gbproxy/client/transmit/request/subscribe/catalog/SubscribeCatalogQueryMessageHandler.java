package io.github.lunasaw.gbproxy.client.transmit.request.subscribe.catalog;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSubscribeEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRegistry;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestProcessor;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;

/**
 * 处理设备通道订阅消息（rootType=Query, cmdType=Catalog, method=SUBSCRIBE）。
 *
 * <p>v1.5.0 改造：协议层同步发 200 OK + 维护内部 {@link SubscribeRegistry}，
 * 然后发布 {@link ClientSubscribeEvent}，业务方通过 {@code SubscribeListener.onCatalogSubscribe} 接收。
 * 业务方主动催发 NOTIFY 时使用 {@code ClientCommandSender.sendCatalogCommand}。
 *
 * @author luna
 */
@Component("subscribeCatalogQueryMessageHandler")
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class SubscribeCatalogQueryMessageHandler extends SubscribeHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.CATALOG.getType();

    private final ApplicationEventPublisher publisher;

    @Override
    public String getRootType() {
        return MessageHandler.QUERY;
    }

    @Override
    public String getMethod() {
        return SubscribeRequestProcessor.METHOD;
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
        if (fromDevice == null || !userId.equals(fromDevice.getUserId())) {
            return;
        }

        DeviceQuery deviceQuery = parseXml(DeviceQuery.class);
        if (deviceQuery == null) {
            log.warn("解析 Catalog 订阅请求失败");
            return;
        }
        // 协议层内化订阅注册（v1.5.0：替代原 SubscribeRequestHandler.putSubscribe）
        SubscribeRegistry.put(deviceQuery.getDeviceId(), subscribeInfo);

        // 同步回 200 OK（毫秒级，必须在 listener 通知之前）
        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
        ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, "OK", contentTypeHeader, event, expiresHeader);

        // 异步通知业务（业务方按需主动发 Catalog NOTIFY）
        publisher.publishEvent(new ClientSubscribeEvent(this, userId, sipId, subscribeInfo.getExpires(), deviceQuery));
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
