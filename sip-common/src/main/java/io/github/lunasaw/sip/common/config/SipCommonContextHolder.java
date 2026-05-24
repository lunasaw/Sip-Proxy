package io.github.lunasaw.sip.common.config;

import io.github.lunasaw.sip.common.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 静态持有 {@link SipCommonProperties}，为非 Spring 容器管理的工具类（如静态构造的 FromDevice、
 * 静态分发的 SipMessageTransmitter）提供 user-agent 等配置项的访问入口。
 *
 * <p>未启用 Spring 上下文（如纯单元测试）或 Bean 尚未注入时，{@link #getUserAgent()} 退化为
 * {@link Constant#AGENT}，避免 NPE。
 *
 * @author luna
 */
@Component
public class SipCommonContextHolder {

    private static volatile SipCommonProperties properties;

    @Autowired
    public SipCommonContextHolder(SipCommonProperties properties) {
        SipCommonContextHolder.properties = properties;
    }

    /**
     * 读取 user-agent 配置。Bean 未注入或值为空白时退化为 {@link Constant#AGENT}。
     */
    public static String getUserAgent() {
        SipCommonProperties props = properties;
        if (props == null) {
            return Constant.AGENT;
        }
        String userAgent = props.getUserAgent();
        if (userAgent == null || userAgent.isBlank()) {
            return Constant.AGENT;
        }
        return userAgent;
    }
}
