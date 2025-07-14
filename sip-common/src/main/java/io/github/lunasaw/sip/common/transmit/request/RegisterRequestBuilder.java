package io.github.lunasaw.sip.common.transmit.request;

import java.text.ParseException;
import java.util.UUID;

import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;

import org.springframework.util.DigestUtils;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;

/**
 * REGISTER请求构建器
 *
 * @author luna
 */
public class RegisterRequestBuilder extends AbstractSipRequestBuilder {

    /**
     * 创建REGISTER请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param expires 过期时间
     * @param callId 呼叫ID
     * @return REGISTER请求
     */
    public Request buildRegisterRequest(FromDevice fromDevice, ToDevice toDevice, Integer expires, String callId) {
        SipMessage sipMessage = SipMessage.getRegisterBody();
        sipMessage.setMethod(Request.REGISTER);
        sipMessage.setCallId(callId);

        // 临时设置expires到toDevice，用于构建请求
        Integer originalExpires = toDevice.getExpires();
        try {
            toDevice.setExpires(expires);
            return build(fromDevice, toDevice, sipMessage);
        } finally {
            // 恢复原始expires
            toDevice.setExpires(originalExpires);
        }
    }

    /**
     * 创建带认证的REGISTER请求
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param callId 呼叫ID
     * @param expires 过期时间
     * @param www 认证头
     * @return 带认证的REGISTER请求
     */
    public Request buildRegisterRequestWithAuth(FromDevice fromDevice, ToDevice toDevice, String callId, Integer expires, WWWAuthenticateHeader www) {
        Request registerRequest = buildRegisterRequest(fromDevice, toDevice, expires, callId);
        URI requestURI = registerRequest.getRequestURI();

        String userId = toDevice.getUserId();
        String password = toDevice.getPassword();

        if (www == null) {
            try {
                AuthorizationHeader authorizationHeader = SipRequestUtils.createAuthorizationHeader("Digest");
                String username = fromDevice.getUserId();
                authorizationHeader.setUsername(username);
                authorizationHeader.setURI(requestURI);
                authorizationHeader.setAlgorithm("MD5");
                registerRequest.addHeader(authorizationHeader);
                return registerRequest;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        String realm = www.getRealm();
        String nonce = www.getNonce();
        String scheme = www.getScheme();
        String qop = www.getQop();

        String cNonce = null;
        String nc = "00000001";
        if (qop != null) {
            if ("auth".equalsIgnoreCase(qop)) {
                cNonce = UUID.randomUUID().toString();
            } else if ("auth-int".equalsIgnoreCase(qop)) {
                // TODO: 实现auth-int逻辑
            }
        }

        String HA1 = DigestUtils.md5DigestAsHex((userId + ":" + realm + ":" + password).getBytes());
        String HA2 = DigestUtils.md5DigestAsHex((Request.REGISTER + ":" + requestURI.toString()).getBytes());

        StringBuilder reStr = new StringBuilder(200);
        reStr.append(HA1);
        reStr.append(":");
        reStr.append(nonce);
        reStr.append(":");
        if (qop != null) {
            reStr.append(nc);
            reStr.append(":");
            reStr.append(cNonce);
            reStr.append(":");
            reStr.append(qop);
            reStr.append(":");
        }
        reStr.append(HA2);

        String RESPONSE = DigestUtils.md5DigestAsHex(reStr.toString().getBytes());

        AuthorizationHeader authorizationHeader = SipRequestUtils.createAuthorizationHeader(
            scheme, userId, requestURI, realm, nonce, qop, cNonce, RESPONSE);
        registerRequest.addHeader(authorizationHeader);

        return registerRequest;
    }

    @Override
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 添加User-Agent头部
        UserAgentHeader userAgentHeader = SipRequestUtils.createUserAgentHeader(fromDevice.getAgent());
        request.addHeader(userAgentHeader);

        // 添加Contact头部
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(fromDevice.getUserId(), fromDevice.getHostAddress());
        request.addHeader(contactHeader);

        // 添加Expires头部
        if (toDevice.getExpires() != null) {
            ExpiresHeader expiresHeader = SipRequestUtils.createExpiresHeader(toDevice.getExpires());
            request.addHeader(expiresHeader);
        }
    }
}