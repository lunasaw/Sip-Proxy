package io.github.lunasaw.gbproxy.server.transmit.request.register;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import javax.sip.RequestEvent;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;

import java.util.ArrayList;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOfflineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceOnlineEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterChallengeEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import org.springframework.stereotype.Component;

import com.luna.common.date.DateUtils;

import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gbproxy.server.transmit.request.ServerAbstractSipRequestProcessor;
import io.github.lunasaw.sip.common.entity.GbSipDate;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Server模块REGISTER请求处理器
 * 只负责SIP协议层面的处理，业务逻辑通过ServerRegisterProcessorHandler接口实现
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
    @Lazy
    private ServerRegisterProcessorHandler serverRegisterProcessorHandler;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 处理REGISTER请求
     * 只负责SIP协议层面的处理，业务逻辑通过ServerRegisterProcessorHandler接口实现
     *
     * @param evt 请求事件
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest)evt.getRequest();
            // 解析协议层面的信息
            int expires = request.getExpires().getExpires();
            boolean isRegister = expires > 0;
            String userId = SipUtils.getUserIdFromFromHeader(request);

            log.debug("处理{}请求：用户ID = {}, 过期时间 = {}", isRegister ? "注册" : "注销", userId, expires);

            // 构建注册信息
            RegisterInfo registerInfo = buildRegisterInfo(request, expires);
            SipTransaction sipTransaction = SipUtils.getSipTransaction(request);

            // 处理注销请求
            if (!isRegister) {
                publisher.publishEvent(new DeviceOfflineEvent(this, userId, registerInfo, sipTransaction));
                return;
            }

            // 处理注册请求
            processRegisterRequest(evt, request, userId, registerInfo, sipTransaction);

        } catch (Exception e) {
            log.error("处理REGISTER请求异常：evt = {}", evt, e);
        }
    }

    /**
     * 处理注册请求
     */
    private void processRegisterRequest(RequestEvent evt, SIPRequest request, String userId, RegisterInfo registerInfo, SipTransaction sipTransaction) {
        // 检查是否为续订请求
        SipTransaction existingTransaction = serverRegisterProcessorHandler.getDeviceTransaction(userId);
        String callId = SipUtils.getCallId(request);

        if (existingTransaction != null && callId.equals(existingTransaction.getCallId())) {
            // 续订请求，直接响应成功
            List<Header> okHeaderList = getRegisterOkHeaderList(request);
            ResponseCmd.response(Response.OK).phrase("OK").requestEvent(evt).headers(okHeaderList).send();
            publisher.publishEvent(new DeviceOnlineEvent(this, userId, sipTransaction));
            return;
        }

        // 处理首次注册认证
        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (authHead == null) {
            // 发送401认证挑战
            sendAuthChallenge(evt, userId);
            return;
        }

        // 验证密码
        if (!serverRegisterProcessorHandler.validatePassword(userId, extractPassword(request), evt)) {
            // 密码验证失败，返回403
            log.warn("REGISTER请求密码验证失败：用户ID = {}", userId);
            ResponseCmd.sendResponse(Response.FORBIDDEN, "wrong password", evt);
            return;
        }

        // 注册成功
        List<Header> okHeaderList = getRegisterOkHeaderList(request);
        ResponseCmd.response(Response.OK).phrase("OK").requestEvent(evt).headers(okHeaderList).send();
        publisher.publishEvent(new DeviceRegisterEvent(this, userId, registerInfo));
        publisher.publishEvent(new DeviceOnlineEvent(this, userId, sipTransaction));
    }

    /**
     * 发送认证挑战
     */
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

    /**
     * 从请求中提取密码信息
     */
    private String extractPassword(SIPRequest request) {
        // 这里可以从请求中提取密码相关信息
        // 具体实现根据实际需求确定
        return "";
    }

    /**
     * 构建注册信息
     */
    private RegisterInfo buildRegisterInfo(SIPRequest request, int expires) {
        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setExpire(expires);
        registerInfo.setRegisterTime(DateUtils.getCurrentDate());

        // 获取传输协议
        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        registerInfo.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        // 获取地址信息
        String receiveIp = request.getLocalAddress().getHostAddress();
        RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request);

        registerInfo.setLocalIp(receiveIp);
        registerInfo.setRemotePort(remoteAddressInfo.getPort());
        registerInfo.setRemoteIp(remoteAddressInfo.getIp());

        return registerInfo;
    }

    private List<Header> getRegisterOkHeaderList(Request request) {

        List<Header> list = new ArrayList<>();
        // 添加date头
        SIPDateHeader dateHeader = new SIPDateHeader();
        // 使用自己修改的
        GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(gbSipDate);
        list.add(dateHeader);

        // 添加Contact头
        list.add(request.getHeader(ContactHeader.NAME));
        // 添加Expires头
        list.add(request.getExpires());
        return list;

    }

}
