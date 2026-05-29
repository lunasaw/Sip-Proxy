package io.github.lunasaw.gb28181.common.transmit.cmd;

import com.luna.common.check.Assert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommandStrategyFactory {

    private final Map<String, CommandStrategy> strategyMap;

    public CommandStrategyFactory(List<CommandStrategy> strategies) {
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(
                s -> s.getRole() + ":" + s.getCommandType(),
                s -> s,
                (a, b) -> { throw new IllegalStateException("重复策略: " + a.getRole() + ":" + a.getCommandType()); }
            ));
    }

    public CommandStrategy getStrategy(String role, String commandType) {
        CommandStrategy s = strategyMap.get(role + ":" + commandType);
        Assert.notNull(s, "未找到策略: " + role + ":" + commandType);
        return s;
    }
}
