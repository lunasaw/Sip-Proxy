package io.github.lunasaw.sipgateway.core.api;

import java.util.Collection;

/**
 * 协议模块 SPI：每个协议适配器必须提供一个 ProtocolModule 实现，向核心声明命名空间和命令清单。
 *
 * @author luna
 */
public interface ProtocolModule {
    /**
     * 协议命名空间，必须与 commandSpecs 的 type 第一段一致。
     * 例如 "gb28181"、"onvif"、"gt1078"。
     */
    String protocol();

    /**
     * 该协议的全部静态命令表。注解白名单方法独立扫描。
     */
    Collection<CommandSpec> commandSpecs();

    /**
     * 启动期注册顺序，一般保持 0；若需为 fail-fast 调整覆盖优先级再使用。
     */
    default int order() {
        return 0;
    }
}
