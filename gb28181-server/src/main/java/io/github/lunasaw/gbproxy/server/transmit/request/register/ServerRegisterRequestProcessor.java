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
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOfflineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOnlineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterChallengeEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
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
                publisher.publishEvent(new DeviceOfflineEvent(this, userId, registerInfo, sipTransaction));
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
        publisher.publishEvent(new DeviceRegisterEvent(this, userId, registerInfo));
        publisher.publishEvent(new DeviceOnlineEvent(this, userId, sipTransaction));
    }

    private void sendAuthChallenge(RequestEvent evt, String userId) {
        try {
            String nonce = DigestServerAuthenticationHelper.generateNonce();
            WWWAuthenticateHeader wwwAuthenticateHeader =
                    SipRequestUtils.createWWWAuthenticateHeader(DigestServerAuthenticationHelper.DEFAULT_SCHEME,
                            "3402000000", nonce, DigestServerAuthenticationHelper.DEFAULT_ALGORITHM);

            ResponseCmd.response(Response.UNAUTHORIZED).phrase("Unauthorized").requestEvent(evt).header(wwwAuthenticateHeader).send();
            publisher.publishEvent(new DeviceRegisterChallengeEvent(this, userId));
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

        return registerInfo;
    }

    private List<Header> getRegisterOkHeaderList(Request request) {
        List<Header> list = new ArrayList<>();
        SIPDateHeader dateHeader = new SIPDateHeader();
        GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(gbSipDate);
        list.add(dateHeader);
        list.add(request.getHeader(ContactHeader.NAME));
        list.add(request.getExpires());
        return list;
    }
}
