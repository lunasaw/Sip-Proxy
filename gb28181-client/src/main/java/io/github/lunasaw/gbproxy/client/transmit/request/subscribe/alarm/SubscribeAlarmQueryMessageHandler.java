package io.github.lunasaw.gbproxy.client.transmit.request.subscribe.alarm;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientAlarmSubscribeEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestProcessor;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;

/**
 * GB28181-2022 §9.11.1 报警事件订阅请求 (rootType=Query, cmdType=Alarm，method=SUBSCRIBE)
 *
 * 客户端收到平台报警订阅 → 200 OK + 发布 {@link ClientAlarmSubscribeEvent}。
 *
 * @author luna
 */
@Component("subscribeAlarmQueryMessageHandler")
@Slf4j
@Getter
@Setter
public class SubscribeAlarmQueryMessageHandler extends SubscribeHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.ALARM.getType();

    private final ApplicationEventPublisher publisher;

    public SubscribeAlarmQueryMessageHandler(@Lazy SubscribeRequestHandler subscribeRequestHandler,
                                              ClientDeviceSupplier deviceSupplier,
                                              ApplicationEventPublisher publisher) {
        super(subscribeRequestHandler, deviceSupplier);
        this.publisher = publisher;
    }

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
        SIPRequest request = (SIPRequest) event.getRequest();
        SubscribeInfo subscribeInfo = new SubscribeInfo(request, deviceSession.getSipId());

        DeviceAlarmQuery query = parseXml(DeviceAlarmQuery.class);
        if (query == null) {
            log.warn("解析报警订阅请求失败");
            return;
        }
        subscribeRequestHandler.putSubscribe(query.getDeviceId(), subscribeInfo);

        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
        ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, "OK", contentTypeHeader, event, expiresHeader);

        publisher.publishEvent(new ClientAlarmSubscribeEvent(this,
                deviceSession.getUserId(), deviceSession.getSipId(), subscribeInfo.getExpires(), query));
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
