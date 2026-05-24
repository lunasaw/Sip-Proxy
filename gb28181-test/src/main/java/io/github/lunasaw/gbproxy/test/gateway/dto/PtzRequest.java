package io.github.lunasaw.gbproxy.test.gateway.dto;

import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import lombok.Data;

@Data
public class PtzRequest {
    private String deviceId;
    private PtzCmdEnum cmd;
    private Integer speed = 128;
}
