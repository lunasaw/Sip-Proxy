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

    /**
     * 1.7.0：BYE dialog-aware 入口。BYE 不再需要 FromDevice / ToDevice —— 信息全部从 dialog 取回。
     *
     * @param role   "client" / "server"
     * @param callId INVITE 阶段记录的 Call-ID
     */
    public static CommandContext forBye(String role, String callId) {
        Map<String, Object> extras = new HashMap<>();
        if (callId != null) extras.put("callId", callId);
        return CommandContext.builder()
            .role(role).commandType("BYE")
            .extras(extras)
            .build();
    }

    /**
     * 1.7.0：SUBSCRIBE 续订 / 退订 dialog-aware 入口。
     *
     * @param role    "client" / "server"
     * @param callId  初始 SUBSCRIBE 的 Call-ID
     * @param content body（XML），通常与初始 SUBSCRIBE 相同；可为 null
     * @param expires 续订时长（秒）；0 表示退订
     */
    public static CommandContext forSubscribeRefresh(String role, String callId, String content, int expires) {
        Map<String, Object> extras = new HashMap<>();
        if (callId != null) extras.put("callId", callId);
        extras.put("expires", expires);
        return CommandContext.builder()
            .role(role).commandType("SUBSCRIBE_REFRESH")
            .content(content)
            .extras(extras)
            .build();
    }

    public String getCallId() {
        return (String) extras.get("callId");
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
