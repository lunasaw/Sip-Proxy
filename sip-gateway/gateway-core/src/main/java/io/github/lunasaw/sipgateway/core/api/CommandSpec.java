package io.github.lunasaw.sipgateway.core.api;

import java.util.List;

/**
 * 描述一条静态命令映射（表驱动核心数据结构）。
 *
 * @param type        命令 type，如 "gb28181.Query.Catalog"（必须以 ProtocolModule#protocol() + "." 开头）
 * @param senderClass 启动期通过 ApplicationContext.getBean(senderClass) 解析；
 *                    GB28181 时 = ServerCommandSender.class，未来 ONVIF = OnvifCommandClient.class
 * @param methodName  例如 "deviceCatalogQuery"
 * @param bindings    参数绑定列表（顺序 = 方法形参顺序）
 * @author luna
 */
public record CommandSpec(
        String type,
        Class<?> senderClass,
        String methodName,
        List<ParamBinding> bindings
) {}
