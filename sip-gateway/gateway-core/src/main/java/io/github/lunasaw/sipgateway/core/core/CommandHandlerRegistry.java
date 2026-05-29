package io.github.lunasaw.sipgateway.core.core;

import io.github.lunasaw.sipgateway.core.api.*;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 命令处理器注册表：跨协议聚合，启动期 fail-fast。
 *
 * <p>分两步装配避免循环依赖：
 * <ol>
 *   <li>构造期：注册 ProtocolModule 静态命令表（不触发其他 bean 创建）</li>
 *   <li>{@link SmartInitializingSingleton#afterSingletonsInstantiated()}：所有 singleton 创建完后扫描
 *       {@code @CommandMapping} 注解方法。这避免了 ctx.getBeansWithAnnotation 在 bean 创建过程中
 *       触发 GatewayDispatchController（依赖本 Registry）的循环依赖。</li>
 * </ol>
 *
 * @author luna
 */
@Component
public class CommandHandlerRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerRegistry.class);

    private final ApplicationContext ctx;
    private final GatewayProperties props;
    private final Map<String, CommandHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, String> typeOwner = new ConcurrentHashMap<>();
    private final Set<String> protocols = ConcurrentHashMap.newKeySet();

    public CommandHandlerRegistry(ApplicationContext ctx,
                                  GatewayProperties props,
                                  List<ProtocolModule> modules) {
        this.ctx = ctx;
        this.props = props;
        registerProtocolModules(modules);
    }

    /**
     * 步骤 1（构造期）：注册各 ProtocolModule 静态表。
     * <p>仅依赖入参 modules，不会触发其他 bean 创建。
     */
    private void registerProtocolModules(List<ProtocolModule> modules) {
        List<ProtocolModule> sorted = modules.stream()
                .sorted(Comparator.comparingInt(ProtocolModule::order))
                .toList();

        for (ProtocolModule m : sorted) {
            protocols.add(m.protocol());
            for (CommandSpec spec : m.commandSpecs()) {
                if (!spec.type().startsWith(m.protocol() + ".")) {
                    throw new IllegalStateException(
                            "ProtocolModule '" + m.protocol() + "' declared spec '"
                                    + spec.type() + "' not under its namespace");
                }
                if (typeOwner.containsKey(spec.type())) {
                    throw new IllegalStateException(
                            "Duplicate type '" + spec.type() + "': "
                                    + typeOwner.get(spec.type()) + " vs " + m.protocol());
                }
                Object sender = ctx.getBean(spec.senderClass());
                handlers.put(spec.type(), new ReflectiveCommandHandler(spec, sender, props.getNodeId()));
                typeOwner.put(spec.type(), m.protocol());
            }
        }
    }

    /**
     * 步骤 2（所有 singleton 创建完后）：扫描 @CommandMapping 注解方法。
     */
    @Override
    public void afterSingletonsInstantiated() {
        for (Object bean : ctx.getBeansWithAnnotation(Component.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                CommandMapping ann = method.getAnnotation(CommandMapping.class);
                if (ann == null) {
                    continue;
                }
                if (method.getParameterCount() != 1
                        || method.getReturnType() != String.class) {
                    throw new IllegalStateException(
                            "@CommandMapping method must have signature: (GatewayCommand) -> String. Found: "
                                    + method);
                }
                if (typeOwner.containsKey(ann.value()) && !ann.overrideTable()) {
                    log.warn("@CommandMapping('{}') silently overrides table entry from '{}'. "
                                    + "Set overrideTable=true to declare intent.",
                            ann.value(), typeOwner.get(ann.value()));
                }
                handlers.put(ann.value(), new MethodInvokerHandler(ann.value(), bean, method, props.getNodeId()));
                typeOwner.put(ann.value(), "annotation:" + bean.getClass().getSimpleName());
            }
        }
        log.info("CommandHandlerRegistry ready: {} types from {} protocols", handlers.size(), protocols.size());
    }

    public CommandHandler require(String type) {
        CommandHandler h = handlers.get(type);
        if (h == null) {
            throw new ResponseStatusException(NOT_FOUND, "unknown command type: " + type);
        }
        return h;
    }

    public Set<String> getKnownProtocols() {
        return Set.copyOf(protocols);
    }
}
