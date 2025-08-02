package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy;

import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.MessageCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.SubscribeCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.NotifyCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.InviteCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.ByeCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.AckCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.InfoCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl.RegisterCommandStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端SIP消息类型策略工厂
 * 管理和获取不同类型的SIP消息处理策略
 * 符合SIP协议架构要求，处理MESSAGE、SUBSCRIBE、NOTIFY、INVITE、BYE、ACK等SIP消息类型
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ClientCommandStrategyFactory {

    private static final Map<String, ClientCommandStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    static {
        // 只保留SIP基础协议的消息类型策略
        STRATEGY_MAP.put("MESSAGE", new MessageCommandStrategy());
        STRATEGY_MAP.put("SUBSCRIBE", new SubscribeCommandStrategy());
        STRATEGY_MAP.put("NOTIFY", new NotifyCommandStrategy());
        STRATEGY_MAP.put("INVITE", new InviteCommandStrategy());
        STRATEGY_MAP.put("BYE", new ByeCommandStrategy());
        STRATEGY_MAP.put("ACK", new AckCommandStrategy());
        STRATEGY_MAP.put("INFO", new InfoCommandStrategy());
        STRATEGY_MAP.put("REGISTER", new RegisterCommandStrategy());

        log.info("客户端SIP消息类型策略工厂初始化完成，已注册策略: {}", STRATEGY_MAP.keySet());
    }

    // 只保留基础SIP方法的获取接口
    public static ClientCommandStrategy getStrategy(String sipMethod) {
        ClientCommandStrategy strategy = STRATEGY_MAP.get(sipMethod);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到SIP消息类型策略: " + sipMethod);
        }
        return strategy;
    }

    public static ClientCommandStrategy getMessageStrategy() {
        return getStrategy("MESSAGE");
    }

    public static ClientCommandStrategy getSubscribeStrategy() {
        return getStrategy("SUBSCRIBE");
    }

    public static ClientCommandStrategy getNotifyStrategy() {
        return getStrategy("NOTIFY");
    }

    public static ClientCommandStrategy getInviteStrategy() {
        return getStrategy("INVITE");
    }

    public static ClientCommandStrategy getByeStrategy() {
        return getStrategy("BYE");
    }

    public static ClientCommandStrategy getAckStrategy() {
        return getStrategy("ACK");
    }

    public static ClientCommandStrategy getInfoStrategy() {
        return getStrategy("INFO");
    }

    public static ClientCommandStrategy getRegisterStrategy() {
        return getStrategy("REGISTER");
    }

    // 删除自定义注册、移除、查询等非必要方法，只保留基础功能
}