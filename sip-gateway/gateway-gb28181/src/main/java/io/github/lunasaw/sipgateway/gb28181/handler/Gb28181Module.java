package io.github.lunasaw.sipgateway.gb28181.handler;

import io.github.lunasaw.sipgateway.core.api.CommandSpec;
import io.github.lunasaw.sipgateway.core.api.ProtocolModule;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * GB28181 协议模块：自报命名空间 + 暴露命令清单。
 *
 * @author luna
 */
@Component
public class Gb28181Module implements ProtocolModule {

    @Override
    public String protocol() {
        return "gb28181";
    }

    @Override
    public Collection<CommandSpec> commandSpecs() {
        return Gb28181CommandSpecs.declare();
    }
}
