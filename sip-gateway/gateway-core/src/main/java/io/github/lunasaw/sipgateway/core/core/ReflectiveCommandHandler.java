package io.github.lunasaw.sipgateway.core.core;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.CommandSpec;
import io.github.lunasaw.sipgateway.core.api.ParamBinding;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 表驱动 handler：启动期为每条 CommandSpec 构造一个实例。
 *
 * @author luna
 */
public final class ReflectiveCommandHandler implements CommandHandler {

    private final CommandSpec spec;
    private final Object sender;
    private final Method targetMethod;
    private final String nodeId;

    public ReflectiveCommandHandler(CommandSpec spec, Object sender, String nodeId) {
        this.spec = spec;
        this.sender = sender;
        this.nodeId = nodeId;
        this.targetMethod = findMethod(sender.getClass(), spec.methodName(), spec.bindings().size());
    }

    @Override
    public String type() {
        return spec.type();
    }

    @Override
    public GatewayCommandResult handle(GatewayCommand cmd) {
        Object[] args = bindArgs(cmd);
        try {
            String correlationId = (String) targetMethod.invoke(sender, args);
            return new GatewayCommandResult(correlationId, spec.type(), nodeId);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Command execution failed: " + spec.type(), cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot invoke method: " + spec.methodName(), e);
        }
    }

    private Object[] bindArgs(GatewayCommand cmd) {
        Object[] args = new Object[spec.bindings().size()];
        for (int i = 0; i < args.length; i++) {
            ParamBinding b = spec.bindings().get(i);
            Object raw = switch (b.source()) {
                case "deviceId" -> cmd.deviceId();
                case "callId" -> Optional.ofNullable(cmd.payload().get("callId"))
                        .orElse(cmd.deviceId());  // 兼容
                default -> cmd.payload().get(b.fieldName());
            };
            if (raw == null && b.defaultValue() != null) {
                raw = b.defaultValue();
            }
            if (raw == null) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "missing field: " + b.fieldName() + " for type " + spec.type());
            }
            args[i] = PayloadCodec.convert(raw, b.targetType());
        }
        return args;
    }

    private Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new IllegalStateException(
                "Method not found: " + clazz.getName() + "." + methodName + "(" + paramCount + " params)");
    }
}
