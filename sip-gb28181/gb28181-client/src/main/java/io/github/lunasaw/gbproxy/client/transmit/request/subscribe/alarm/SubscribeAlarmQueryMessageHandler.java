package io.github.lunasaw.gbproxy.client.transmit.request.subscribe.alarm;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSubscribeEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRegistry;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestProcessor;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;

/**
 * GB28181-2022 §9.11.1 报警事件订阅请求 (rootType=Query, cmdType=Alarm，method=SUBSCRIBE)。
 *
 * <p>v1.5.0 改造：协议层同步发 200 OK + 维护内部 {@link SubscribeRegistry}，
 * 然后发布 {@link ClientSubscribeEvent}，业务方通过 {@code SubscribeListener.onAlarmSubscribe} 接收。
 *
 * @author luna
 */
@Component("subscribeAlarmQueryMessageHandler")
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class SubscribeAlarmQueryMessageHandler extends SubscribeHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.ALARM.getType();

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
        SIPRequest request = (SIPRequest) event.getRequest();
        SubscribeInfo subscribeInfo = new SubscribeInfo(request, deviceSession.getSipId());

        DeviceAlarmQuery query = parseXml(DeviceAlarmQuery.class);
        if (query == null) {
            log.warn("解析报警订阅请求失败");
            return;
        }
        // 协议层内化订阅注册（v1.5.0：替代原 SubscribeRequestHandler.putSubscribe）
        SubscribeRegistry.put(query.getDeviceId(), subscribeInfo);

        // 同步回 200 OK（毫秒级，必须在 listener 通知之前）
        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
        ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, "OK", contentTypeHeader, event, expiresHeader);

        // 异步通知业务（listener 慢/异步都不影响 SIP 事务）
        publisher.publishEvent(new ClientSubscribeEvent(this,
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
