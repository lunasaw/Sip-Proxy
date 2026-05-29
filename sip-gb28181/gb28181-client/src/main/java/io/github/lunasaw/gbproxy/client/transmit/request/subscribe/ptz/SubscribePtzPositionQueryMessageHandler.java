package io.github.lunasaw.gbproxy.client.transmit.request.subscribe.ptz;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.PTZPositionQuery;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSubscribeEvent;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeHandlerAbstract;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRegistry;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestProcessor;
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
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Response;

/**
 * GB28181-2022 §9.11.1 / §A.2.4.13 PTZ 精准位置变化事件订阅请求
 * (rootType=Query, cmdType=PTZPosition, method=SUBSCRIBE)。
 *
 * <p>协议层同步发 200 OK + 维护内部 {@link SubscribeRegistry}，
 * 然后发布 {@link ClientSubscribeEvent}，业务方通过
 * {@code SubscribeListener.onPtzPositionSubscribe} 接收。
 *
 * @author luna
 */
@Component("subscribePtzPositionQueryMessageHandler")
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class SubscribePtzPositionQueryMessageHandler extends SubscribeHandlerAbstract {

    public static final String CMD_TYPE = CmdTypeEnum.PTZ_POSITION.getType();

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

        PTZPositionQuery query = parseXml(PTZPositionQuery.class);
        if (query == null) {
            log.warn("解析 PTZ 精准位置订阅请求失败");
            return;
        }
        SubscribeRegistry.put(query.getDeviceId(), subscribeInfo);

        ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(subscribeInfo.getExpires());
        ContentTypeHeader contentTypeHeader = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, "OK", contentTypeHeader, event, expiresHeader);

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
