package io.github.lunasaw.sipgateway.core.web;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Gateway 协议中立分发控制器。
 *
 * @author luna
 */
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayDispatchController {

    private static final Logger log = LoggerFactory.getLogger(GatewayDispatchController.class);

    private final GatewayProperties props;
    private final CommandHandlerRegistry registry;

    @PostMapping("/command")
    public GatewayCommandResult dispatch(@RequestBody GatewayCommand cmd) {
        String type = cmd.type();
        Set<String> knownProtocols = registry.getKnownProtocols();

        // 1.8.0 兼容 shim：无协议前缀时默认补 gb28181.（1.10.0 移除）
        boolean hasPrefix = type.indexOf('.') > 0
                && knownProtocols.contains(type.substring(0, type.indexOf('.')));

        if (!hasPrefix) {
            log.warn("type '{}' missing protocol prefix; falling back to 'gb28181.{}'. "
                            + "This compat shim will be removed in 1.10.0.", type, type);
            type = "gb28181." + type;
            cmd = cmd.withType(type);
        }
        return registry.require(type).handle(cmd);
    }

    @GetMapping("/whoami")
    public Map<String, String> whoami() {
        return Map.of("nodeId", props.getNodeId());
    }
}
