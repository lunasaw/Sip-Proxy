package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.message.Response;

/**
 * 客户端收到 INVITE 后异步通过事件回包：构造测试 SDP，附带 Contact 头取回事务上下文发 200 OK。
 *
 * <p>RFC 3261 §13.3.1.4 要求 INVITE 的 2xx 响应必须携带 Contact 头建立会话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestInviteRequestHandler {

    private static final String TEST_SDP =
        "v=0\r\n" +
        "o=34020000001320000001 0 0 IN IP4 127.0.0.1\r\n" +
        "s=Play\r\n" +
        "c=IN IP4 127.0.0.1\r\n" +
        "t=0 0\r\n" +
        "m=video 10000 RTP/AVP 96\r\n" +
        "a=rtpmap:96 PS/90000\r\n" +
        "y=0100000001\r\n";

    private final ClientDeviceSupplier clientDeviceSupplier;

    @EventListener
    public void onClientInvite(ClientInviteEvent event) {
        SipTransactionRegistry.TransactionContextInfo ctx =
                SipTransactionRegistry.getContext(event.getTransactionContextKey());
        if (ctx == null) {
            log.warn("INVITE 事务上下文已失效: callId={}", event.getCallId());
            return;
        }
        FromDevice client = clientDeviceSupplier.getClientFromDevice();
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(client.getUserId(), client.getHostAddress());
        ContentTypeHeader contentType = ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, TEST_SDP, contentType, ctx.getOriginalEvent(), (Header) contactHeader);
        log.info("✅ 客户端 INVITE 已回 200 OK: callId={}", event.getCallId());
    }
}
