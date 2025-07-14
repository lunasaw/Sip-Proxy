package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy;

import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.MessageCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.SubscribeCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.InviteCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.ByeCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.AckCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.InfoCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.impl.RegisterCommandStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端SIP消息类型策略工厂
 * 管理和获取不同类型的SIP消息处理策略
 * 符合SIP协议架构要求，处理MESSAGE、SUBSCRIBE、INVITE、BYE、ACK、INFO、REGISTER等SIP消息类型
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ServerCommandStrategyFactory {

    private static final Map<String, ServerCommandStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    static {
        // 注册SIP基础协议的消息类型策略
        STRATEGY_MAP.put("MESSAGE", new MessageCommandStrategy());
        STRATEGY_MAP.put("SUBSCRIBE", new SubscribeCommandStrategy());
        STRATEGY_MAP.put("INVITE", new InviteCommandStrategy());
        STRATEGY_MAP.put("BYE", new ByeCommandStrategy());
        STRATEGY_MAP.put("ACK", new AckCommandStrategy());
        STRATEGY_MAP.put("INFO", new InfoCommandStrategy());
        STRATEGY_MAP.put("REGISTER", new RegisterCommandStrategy());

        log.info("服务端SIP消息类型策略工厂初始化完成，已注册策略: {}", STRATEGY_MAP.keySet());
    }

    /**
     * 获取SIP消息类型策略
     *
     * @param sipMethod SIP方法
     * @return 策略实例
     */
    public static ServerCommandStrategy getStrategy(String sipMethod) {
        ServerCommandStrategy strategy = STRATEGY_MAP.get(sipMethod);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到SIP消息类型策略: " + sipMethod);
        }
        return strategy;
    }

    public static ServerCommandStrategy getMessageStrategy() {
        return getStrategy("MESSAGE");
    }

    public static ServerCommandStrategy getSubscribeStrategy() {
        return getStrategy("SUBSCRIBE");
    }

    public static ServerCommandStrategy getInviteStrategy() {
        return getStrategy("INVITE");
    }

    public static ServerCommandStrategy getByeStrategy() {
        return getStrategy("BYE");
    }

    public static ServerCommandStrategy getAckStrategy() {
        return getStrategy("ACK");
    }

    public static ServerCommandStrategy getInfoStrategy() {
        return getStrategy("INFO");
    }

    public static ServerCommandStrategy getRegisterStrategy() {
        return getStrategy("REGISTER");
    }

    /**
     * 获取所有已注册的策略
     *
     * @return 策略映射
     */
    public static Map<String, ServerCommandStrategy> getAllStrategies() {
        return new ConcurrentHashMap<>(STRATEGY_MAP);
    }

    /**
     * 检查是否支持指定的SIP方法
     *
     * @param sipMethod SIP方法
     * @return 是否支持
     */
    public static boolean isSupported(String sipMethod) {
        return STRATEGY_MAP.containsKey(sipMethod);
    }
}