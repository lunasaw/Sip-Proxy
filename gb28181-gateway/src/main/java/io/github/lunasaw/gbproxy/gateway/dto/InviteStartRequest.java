package io.github.lunasaw.gbproxy.gateway.dto;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import lombok.Data;

@Data
public class InviteStartRequest {
    private String deviceId;
    private String mediaIp;
    private Integer mediaPort;
    private StreamModeEnum streamMode = StreamModeEnum.UDP;
}
