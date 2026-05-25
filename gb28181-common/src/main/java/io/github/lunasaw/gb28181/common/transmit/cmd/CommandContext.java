package io.github.lunasaw.gb28181.common.transmit.cmd;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CommandContext {
    private String role;
    private String commandType;
    private FromDevice fromDevice;
    private ToDevice toDevice;
    private String content;
    private Object body;
    private Event errorEvent;
    private Event okEvent;
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();

    public static CommandContext forRegister(String role, FromDevice from, ToDevice to, int expires) {
        return CommandContext.builder()
            .role(role).commandType("REGISTER")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("expires", expires)))
            .build();
    }

    public static CommandContext forSubscribe(String role, FromDevice from, ToDevice to, SubscribeInfo subscribeInfo, int expires) {
        return CommandContext.builder()
            .role(role).commandType("SUBSCRIBE")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("subscribeInfo", subscribeInfo, "expires", expires)))
            .build();
    }

    public static CommandContext forAckBye(String role, FromDevice from, ToDevice to, String callId, String commandType) {
        Map<String, Object> extras = new HashMap<>();
        if (callId != null) extras.put("callId", callId);
        return CommandContext.builder()
            .role(role).commandType(commandType)
            .fromDevice(from).toDevice(to)
            .extras(extras)
            .build();
    }

    public static CommandContext forInfo(String role, FromDevice from, ToDevice to, String controlBody) {
        return CommandContext.builder()
            .role(role).commandType("INFO")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("controlBody", controlBody)))
            .build();
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, Class<T> type) {
        return type.cast(extras.get(key));
    }
}
