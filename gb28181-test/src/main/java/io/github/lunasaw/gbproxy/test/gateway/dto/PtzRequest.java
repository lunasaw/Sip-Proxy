package io.github.lunasaw.gbproxy.test.gateway.dto;

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import lombok.Data;

@Data
public class PtzRequest {
    private String deviceId;
    private PTZControlEnum cmd;
    private Integer speed = 128;
}
