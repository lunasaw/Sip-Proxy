package io.github.lunasaw.sipgateway.core.core;

import io.github.lunasaw.sipgateway.core.api.*;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 命令处理器注册表：跨协议聚合，启动期 fail-fast。
 *
 * @author luna
 */
@Component
public class CommandHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerRegistry.class);

    private final Map<String, CommandHandler> handlers;
    private final Set<String> knownProtocols;

    public CommandHandlerRegistry(ApplicationContext ctx,
                                  GatewayProperties props,
                                  List<ProtocolModule> modules) {
        Map<String, CommandHandler> all = new HashMap<>();
        Map<String, String> typeOwner = new HashMap<>();
        Set<String> protocols = new HashSet<>();

        // 1) ProtocolModule 注册的静态表
        List<ProtocolModule> sorted = modules.stream()
                .sorted(Comparator.comparingInt(ProtocolModule::order))
                .toList();

        for (ProtocolModule m : sorted) {
            protocols.add(m.protocol());
            for (CommandSpec spec : m.commandSpecs()) {
                // 校验 type 前缀
                if (!spec.type().startsWith(m.protocol() + ".")) {
                    throw new IllegalStateException(
                            "ProtocolModule '" + m.protocol() + "' declared spec '"
                                    + spec.type() + "' not under its namespace");
                }
                // 校验 type 重复
                if (typeOwner.containsKey(spec.type())) {
                    throw new IllegalStateException(
                            "Duplicate type '" + spec.type() + "': "
                                    + typeOwner.get(spec.type()) + " vs " + m.protocol());
                }
                Object sender = ctx.getBean(spec.senderClass());
                all.put(spec.type(), new ReflectiveCommandHandler(spec, sender, props.getNodeId()));
                typeOwner.put(spec.type(), m.protocol());
            }
        }

        // 2) 扫所有 Spring bean 的 @CommandMapping 方法
        for (Object bean : ctx.getBeansWithAnnotation(Component.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                CommandMapping ann = method.getAnnotation(CommandMapping.class);
                if (ann == null) {
                    continue;
                }
                // 校验签名
                if (method.getParameterCount() != 1
                        || method.getReturnType() != String.class) {
                    throw new IllegalStateException(
                            "@CommandMapping method must have signature: (GatewayCommand) -> String. Found: "
                                    + method);
                }
                // 覆盖提示
                if (typeOwner.containsKey(ann.value()) && !ann.overrideTable()) {
                    log.warn("@CommandMapping('{}') silently overrides table entry from '{}'. "
                                    + "Set overrideTable=true to declare intent.",
                            ann.value(), typeOwner.get(ann.value()));
                }
                all.put(ann.value(), new MethodInvokerHandler(ann.value(), bean, method, props.getNodeId()));
                typeOwner.put(ann.value(), "annotation:" + bean.getClass().getSimpleName());
            }
        }

        this.handlers = Map.copyOf(all);
        this.knownProtocols = Set.copyOf(protocols);
        log.info("CommandHandlerRegistry ready: {} types from {} modules", handlers.size(), modules.size());
    }

    public CommandHandler require(String type) {
        CommandHandler h = handlers.get(type);
        if (h == null) {
            throw new ResponseStatusException(NOT_FOUND, "unknown command type: " + type);
        }
        return h;
    }

    public Set<String> getKnownProtocols() {
        return knownProtocols;
    }
}
