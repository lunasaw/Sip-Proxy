package io.github.lunasaw.gbproxy.test.gateway.dto;

import lombok.Data;

@Data
public class ByeRequest {
    private String deviceId;
    private String callId;
}
