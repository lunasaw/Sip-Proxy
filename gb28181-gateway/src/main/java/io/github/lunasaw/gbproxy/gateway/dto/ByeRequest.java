package io.github.lunasaw.gbproxy.gateway.dto;

import lombok.Data;

@Data
public class ByeRequest {
    private String deviceId;
    private String callId;
}
