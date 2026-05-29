package io.github.lunasaw.sipgateway.core.core;

import io.github.lunasaw.sipgateway.core.api.CommandHandler;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 注解方法 handler：@CommandMapping 标注的方法运行期适配。
 *
 * @author luna
 */
public final class MethodInvokerHandler implements CommandHandler {

    private final String type;
    private final Object bean;
    private final Method method;
    private final String nodeId;

    public MethodInvokerHandler(String type, Object bean, Method method, String nodeId) {
        this.type = type;
        this.bean = bean;
        this.method = method;
        this.nodeId = nodeId;
        method.setAccessible(true);
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public GatewayCommandResult handle(GatewayCommand cmd) {
        try {
            String correlationId = (String) method.invoke(bean, cmd);
            return new GatewayCommandResult(correlationId, type, nodeId);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Command execution failed: " + type, cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot invoke method: " + method, e);
        }
    }
}
