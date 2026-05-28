package io.github.lunasaw.gbproxy.server.transmit.request.register;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.sip.RequestEvent;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.Header;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.luna.common.date.DateUtils;

import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.GbSipDate;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerLifecycleEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.constant.SipHeaderConstants;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.request.GbExtensionHeaderDecorator;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * REGISTER 请求处理器：协议层完成 401 挑战 / 200 OK，业务侧通过事件总线感知。
 *
 * <p>密码校验下沉到 {@link ServerDeviceSupplier#authenticate(String, SIPRequest)}，
 * 业务方一般实现为
 * {@code DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, password)}。
 *
 * <p>注：本版本去除了基于 callId 的续订快路径，所有 REGISTER 都走完整 401→Auth→200 OK，
 * 简化状态管理。
 *
 * @author luna
 */
@Getter
@Setter
@Component("serverRegisterRequestProcessor")
@Slf4j
public class ServerRegisterRequestProcessor extends ServerAbstractSipRequestProcessor {

    public static final String METHOD = "REGISTER";

    private String method = METHOD;

    @Autowired
    private ServerDeviceSupplier serverDeviceSupplier;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            int expires = request.getExpires().getExpires();
            boolean isRegister = expires > 0;
            String userId = SipUtils.getUserIdFromFromHeader(request);

            log.debug("处理{}请求：用户ID = {}, 过期时间 = {}", isRegister ? "注册" : "注销", userId, expires);

            RegisterInfo registerInfo = buildRegisterInfo(request, expires);
            SipTransaction sipTransaction = SipUtils.getSipTransaction(request);

            if (!isRegister) {
                publisher.publishEvent(ServerLifecycleEvent.offline(this, userId, registerInfo, sipTransaction));
                return;
            }

            processRegisterRequest(evt, request, userId, registerInfo, sipTransaction);
        } catch (Exception e) {
            log.error("处理REGISTER请求异常：evt = {}", evt, e);
        }
    }

    private void processRegisterRequest(RequestEvent evt, SIPRequest request, String userId,
                                        RegisterInfo registerInfo, SipTransaction sipTransaction) {
        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (authHead == null) {
            sendAuthChallenge(evt, userId);
            return;
        }

        if (!serverDeviceSupplier.authenticate(userId, request)) {
            log.warn("REGISTER鉴权失败：userId = {}", userId);
            ResponseCmd.sendResponse(Response.FORBIDDEN, "Forbidden", evt);
            return;
        }

        List<Header> okHeaderList = getRegisterOkHeaderList(request);
        ResponseCmd.response(Response.OK).phrase("OK").requestEvent(evt).headers(okHeaderList).send();
        publisher.publishEvent(ServerLifecycleEvent.register(this, userId, registerInfo));
        publisher.publishEvent(ServerLifecycleEvent.online(this, userId, sipTransaction));
    }

    private void sendAuthChallenge(RequestEvent evt, String userId) {
        try {
            String nonce = DigestServerAuthenticationHelper.generateNonce();
            WWWAuthenticateHeader wwwAuthenticateHeader =
                    SipRequestUtils.createWWWAuthenticateHeader(DigestServerAuthenticationHelper.DEFAULT_SCHEME,
                            "3402000000", nonce, DigestServerAuthenticationHelper.DEFAULT_ALGORITHM);

            // GBT-28181-2022 附录 I：401 挑战响应也需携带 X-GB-Ver
            List<Header> headers = new ArrayList<>();
            headers.add(wwwAuthenticateHeader);
            Header xGbVer = buildXGbVerHeader();
            if (xGbVer != null) {
                headers.add(xGbVer);
            }

            ResponseCmd.response(Response.UNAUTHORIZED).phrase("Unauthorized").requestEvent(evt).headers(headers).send();
            publisher.publishEvent(ServerLifecycleEvent.challenge(this, userId));
        } catch (Exception e) {
            log.error("发送认证挑战失败：用户ID = {}", userId, e);
        }
    }

    private RegisterInfo buildRegisterInfo(SIPRequest request, int expires) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setExpire(expires);
        registerInfo.setRegisterTime(DateUtils.getCurrentDate());

        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        registerInfo.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        String receiveIp = request.getLocalAddress().getHostAddress();
        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request);

        registerInfo.setLocalIp(receiveIp);
        registerInfo.setRemotePort(remoteAddressInfo.getPort());
        registerInfo.setRemoteIp(remoteAddressInfo.getIp());

        // GBT-28181-2022 附录 I：解析对端协议版本
        Header peerVerHeader = request.getHeader(SipHeaderConstants.X_GB_VER_HEADER);
        if (peerVerHeader != null) {
            registerInfo.setPeerProtocolVersion(extractHeaderValue(peerVerHeader));
        }
        // GBT-28181-2022 §8.3：解析对端 Note / Monitor-User-Identity
        Header noteHeader = request.getHeader(SipHeaderConstants.NOTE_HEADER);
        if (noteHeader != null) {
            registerInfo.setPeerNote(extractHeaderValue(noteHeader));
        }
        Header muiHeader = request.getHeader(SipHeaderConstants.MONITOR_USER_IDENTITY_HEADER);
        if (muiHeader != null) {
            registerInfo.setPeerMonitorUserIdentity(extractHeaderValue(muiHeader));
        }

        return registerInfo;
    }

    /**
     * 把 JAIN-SIP 头域 {@code "Name: value\r\n"} 转换为纯 value，解析失败返回 null。
     */
    private static String extractHeaderValue(Header header) {
        String headerValue = header.toString();
        int colonIdx = headerValue.indexOf(':');
        if (colonIdx <= 0) {
            return null;
        }
        String trimmed = headerValue.substring(colonIdx + 1).trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<Header> getRegisterOkHeaderList(Request request) {
        List<Header> list = new ArrayList<>();
        SIPDateHeader dateHeader = new SIPDateHeader();
        GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(gbSipDate);
        list.add(dateHeader);
        list.add(request.getHeader(ContactHeader.NAME));
        list.add(request.getExpires());

        // GBT-28181-2022 附录 I：200 OK 响应中携带平台 X-GB-Ver
        Header xGbVer = buildXGbVerHeader();
        if (xGbVer != null) {
            list.add(xGbVer);
        }
        return list;
    }

    /**
     * GBT-28181-2022 附录 I：构造 X-GB-Ver 头域，配置缺失时返回 null（向后兼容）。
     */
    private Header buildXGbVerHeader() {
        GbExtensionHeaderDecorator decorator = GbExtensionHeaderDecorator.getInstance();
        if (decorator == null) {
            return null;
        }
        String version = decorator.getProperties().getProtocolVersion();
        if (version == null || version.isBlank()) {
            return null;
        }
        return SipRequestUtils.createHeader(SipHeaderConstants.X_GB_VER_HEADER, version);
    }
}
