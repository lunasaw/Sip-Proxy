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

import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.utils.SipDigestUtils;
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

        // GBT-28181-2022 §8.3：摘要算法按 WWW-Authenticate 头中 algorithm 参数选择，
        // 缺省为 MD5（RFC 3261），SM3 时走国密路径。
        String algorithm = resolveDigestAlgorithm(www);

        if (www == null) {
            try {
                AuthorizationHeader authorizationHeader = SipRequestUtils.createAuthorizationHeader("Digest");
                String username = fromDevice.getUserId();
                authorizationHeader.setUsername(username);
                authorizationHeader.setURI(requestURI);
                authorizationHeader.setAlgorithm(algorithm);
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
            if ("auth".equalsIgnoreCase(qop) || "auth-int".equalsIgnoreCase(qop)) {
                cNonce = UUID.randomUUID().toString();
            }
        }

        String HA1 = SipDigestUtils.digestHex(algorithm, userId + ":" + realm + ":" + password);
        // auth-int: HA2 = digest(method:uri:digest(body))，body 为空时 digest("") 固定值
        String HA2;
        if ("auth-int".equalsIgnoreCase(qop)) {
            String bodyHash = SipDigestUtils.digestHex(algorithm, "");
            HA2 = SipDigestUtils.digestHex(algorithm, Request.REGISTER + ":" + requestURI + ":" + bodyHash);
        } else {
            HA2 = SipDigestUtils.digestHex(algorithm, Request.REGISTER + ":" + requestURI.toString());
        }

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

        String RESPONSE = SipDigestUtils.digestHex(algorithm, reStr.toString());

        AuthorizationHeader authorizationHeader = SipRequestUtils.createAuthorizationHeader(
            scheme, userId, requestURI, realm, nonce, qop, cNonce, RESPONSE);
        try {
            authorizationHeader.setAlgorithm(algorithm);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        registerRequest.addHeader(authorizationHeader);

        return registerRequest;
    }

    /**
     * 解析摘要算法：
     * <ol>
     *   <li>如果 WWW-Authenticate 显式指定 algorithm（MD5 / SM3），按对端要求</li>
     *   <li>否则查 {@code sip.common.signal-auth.algorithm} 配置</li>
     *   <li>都缺失时回落到 MD5（RFC 3261 默认）</li>
     * </ol>
     */
    private String resolveDigestAlgorithm(WWWAuthenticateHeader www) {
        if (www != null && www.getAlgorithm() != null && !www.getAlgorithm().isBlank()) {
            return www.getAlgorithm();
        }
        GbExtensionHeaderDecorator decorator = GbExtensionHeaderDecorator.getInstance();
        if (decorator != null) {
            SipCommonProperties.SignalAuth cfg = decorator.getProperties().getSignalAuth();
            if (cfg.isEnabled() && cfg.getAlgorithm() != null && !cfg.getAlgorithm().isBlank()) {
                return cfg.getAlgorithm();
            }
        }
        return SipDigestUtils.ALGORITHM_MD5;
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

        // GBT-28181-2022 附录 I：协议版本标识 X-GB-Ver
        // GBT-28181-2022 §8.3：信令认证扩展（Note / Monitor-User-Identity，启用时）
        GbExtensionHeaderDecorator decorator = GbExtensionHeaderDecorator.getInstance();
        if (decorator != null) {
            decorator.addXGbVer(request);
            decorator.addNoteHeader(request, fromDevice.getUserId());
            decorator.addMonitorUserIdentity(request, null);
        }
    }
}