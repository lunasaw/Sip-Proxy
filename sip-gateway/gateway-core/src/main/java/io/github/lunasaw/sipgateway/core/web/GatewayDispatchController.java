package io.github.lunasaw.sipgateway.core.web;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gateway 协议中立分发控制器。
 *
 * <p>所有 type 必须是三段式 {@code <protocol>.<Group>.<Name>}，未知 type 直接 404。
 *
 * @author luna
 */
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayDispatchController {

    private final GatewayProperties props;
    private final CommandHandlerRegistry registry;

    @PostMapping("/command")
    public GatewayCommandResult dispatch(@RequestBody GatewayCommand cmd) {
        return registry.require(cmd.type()).handle(cmd);
    }

    @GetMapping("/whoami")
    public Map<String, String> whoami() {
        return Map.of("nodeId", props.getNodeId());
    }
}
