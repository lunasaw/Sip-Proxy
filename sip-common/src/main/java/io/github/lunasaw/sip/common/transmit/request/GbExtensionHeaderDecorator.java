package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.header.Header;
import javax.sip.message.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.config.SipCommonProperties;
import io.github.lunasaw.sip.common.constant.SipHeaderConstants;
import io.github.lunasaw.sip.common.utils.SipDigestUtils;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * GBT-28181-2022 SIP 扩展头域装配器。
 *
 * <p>集中实现：
 * <ul>
 *   <li>附录 I：{@link SipHeaderConstants#X_GB_VER_HEADER X-GB-Ver} 协议版本标识</li>
 *   <li>§8.3：{@link SipHeaderConstants#NOTE_HEADER Note} 摘要扩展（启用时）</li>
 *   <li>§8.3：{@link SipHeaderConstants#MONITOR_USER_IDENTITY_HEADER Monitor-User-Identity}
 *       跨域用户身份链（启用时）</li>
 * </ul>
 *
 * <p>调用入口位于 {@link RegisterRequestBuilder} 与 server 端
 * {@code ServerRegisterRequestProcessor#getRegisterOkHeaderList} / {@code sendAuthChallenge}。
 *
 * @author luna
 */
@Slf4j
@Component
public class GbExtensionHeaderDecorator {

    /** 静态持有，供静态构建器（{@link RegisterRequestBuilder} 等）调用。 */
    private static volatile GbExtensionHeaderDecorator INSTANCE;

    private final SipCommonProperties properties;

    @Autowired
    public GbExtensionHeaderDecorator(SipCommonProperties properties) {
        this.properties = properties;
        INSTANCE = this;
    }

    /**
     * 静态访问入口。
     * 静态请求构建器（无法被 Spring 注入）通过此入口拿到实例。
     * 启动早期或单元测试场景下未注入时返回 null，调用方需做 null 检查。
     */
    public static GbExtensionHeaderDecorator getInstance() {
        return INSTANCE;
    }

    /** 仅供单元测试在没有 Spring 上下文时初始化使用。 */
    static void setInstanceForTesting(GbExtensionHeaderDecorator decorator) {
        INSTANCE = decorator;
    }

    public SipCommonProperties getProperties() {
        return properties;
    }

    /**
     * 附录 I：在 REGISTER 请求或响应消息上附加 {@code X-GB-Ver} 头。
     * 默认值取自 {@code sip.common.protocol-version}，缺省 {@code 3.0}。
     *
     * @param message JAIN-SIP 消息（REGISTER 请求或响应均可）
     */
    public void addXGbVer(Message message) {
        if (message == null) {
            return;
        }
        String version = properties.getProtocolVersion();
        if (StringUtils.isBlank(version)) {
            return;
        }
        Header header = SipRequestUtils.createHeader(SipHeaderConstants.X_GB_VER_HEADER, version);
        message.addHeader(header);
    }

    /**
     * §8.3：在 SIP 消息上附加 {@code Note} 摘要扩展头。
     * 仅当 {@code sip.common.signal-auth.enabled=true} 且 {@code seed} 非空时生效。
     *
     * <p>计算流程：按 {@code sip.common.signal-auth.algorithm}（MD5/SM3）对 {@code seed}
     * 做摘要，再 BASE64 编码，最终拼成 {@code Digest nonce="<b64>", algorithm=<ALG>}。
     *
     * @param message JAIN-SIP 消息
     * @param seed    摘要种子（业务可传 userId / callId / body 等）
     */
    public void addNoteHeader(Message message, String seed) {
        if (message == null || !properties.getSignalAuth().isEnabled()) {
            return;
        }
        String algorithm = properties.getSignalAuth().getAlgorithm();
        if (StringUtils.isBlank(algorithm) || StringUtils.isBlank(seed)) {
            return;
        }
        String nonceBase64 = SipDigestUtils.digestBase64(algorithm, seed);
        String value = String.format("Digest nonce=\"%s\", algorithm=%s", nonceBase64, algorithm);
        Header header = SipRequestUtils.createHeader(SipHeaderConstants.NOTE_HEADER, value);
        message.addHeader(header);
    }

    /**
     * §8.3：在 SIP 消息上附加或扩展 {@code Monitor-User-Identity} 跨域用户身份链。
     *
     * <p>规则：若该信令是由本域用户发起，{@code existingChain} 传 null —— 网关注入
     * {@code <gatewayId>-<userId>[-<userAttribute>]}；若是转发外域来的信令，
     * {@code existingChain} 传旧值 —— 网关在最前面追加 {@code <gatewayId>-} 前缀。
     *
     * @param message       JAIN-SIP 消息
     * @param existingChain 转发场景下传入既有 Monitor-User-Identity 值；本域发起传 null
     */
    public void addMonitorUserIdentity(Message message, String existingChain) {
        if (message == null || !properties.getSignalAuth().isEnabled()) {
            return;
        }
        SipCommonProperties.SignalAuth cfg = properties.getSignalAuth();
        String gatewayId = cfg.getGatewayId();
        if (StringUtils.isBlank(gatewayId)) {
            return;
        }
        String chain;
        if (StringUtils.isNotBlank(existingChain)) {
            chain = gatewayId + SipHeaderConstants.MONITOR_USER_IDENTITY_DELIMITER + existingChain;
        } else {
            String userId = cfg.getUserId();
            if (StringUtils.isBlank(userId)) {
                return;
            }
            StringBuilder sb = new StringBuilder(64)
                    .append(gatewayId)
                    .append(SipHeaderConstants.MONITOR_USER_IDENTITY_DELIMITER)
                    .append(userId);
            if (StringUtils.isNotBlank(cfg.getUserAttribute())) {
                sb.append(SipHeaderConstants.MONITOR_USER_IDENTITY_DELIMITER)
                  .append(cfg.getUserAttribute());
            }
            chain = sb.toString();
        }
        Header header = SipRequestUtils.createHeader(SipHeaderConstants.MONITOR_USER_IDENTITY_HEADER, chain);
        message.addHeader(header);
    }
}
