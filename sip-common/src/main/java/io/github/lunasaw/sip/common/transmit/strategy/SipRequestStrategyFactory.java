package io.github.lunasaw.sip.common.transmit.strategy;

import io.github.lunasaw.sip.common.transmit.strategy.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIP请求策略工厂
 * 管理和获取不同的请求策略
 *
 * @author lin
 */
@Slf4j
public class SipRequestStrategyFactory {

    private static final Map<String, SipRequestStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    static {
        // 初始化默认策略
        STRATEGY_MAP.put("MESSAGE", new MessageRequestStrategy());
        STRATEGY_MAP.put("INVITE", new InviteRequestStrategy());
        STRATEGY_MAP.put("SUBSCRIBE", new SubscribeRequestStrategy());
        STRATEGY_MAP.put("NOTIFY", new NotifyRequestStrategy());
        STRATEGY_MAP.put("BYE", new ByeRequestStrategy());
        STRATEGY_MAP.put("ACK", new AckRequestStrategy());
        STRATEGY_MAP.put("INFO", new InfoRequestStrategy());
    }

    /**
     * 获取请求策略
     *
     * @param method SIP方法名
     * @return 请求策略
     */
    public static SipRequestStrategy getStrategy(String method) {
        return STRATEGY_MAP.get(method.toUpperCase());
    }

    /**
     * 获取注册请求策略
     *
     * @param expires 过期时间
     * @return 注册请求策略
     */
    public static SipRequestStrategy getRegisterStrategy(Integer expires) {
        return new RegisterRequestStrategy(expires);
    }

    /**
     * 注册自定义策略
     *
     * @param method SIP方法名
     * @param strategy 策略实现
     */
    public static void registerStrategy(String method, SipRequestStrategy strategy) {
        STRATEGY_MAP.put(method.toUpperCase(), strategy);
        log.info("注册SIP请求策略: {} -> {}", method, strategy.getClass().getSimpleName());
    }

    /**
     * 移除策略
     *
     * @param method SIP方法名
     */
    public static void removeStrategy(String method) {
        STRATEGY_MAP.remove(method.toUpperCase());
        log.info("移除SIP请求策略: {}", method);
    }
}