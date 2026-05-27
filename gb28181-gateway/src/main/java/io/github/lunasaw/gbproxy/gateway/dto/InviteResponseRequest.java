package io.github.lunasaw.gbproxy.gateway.dto;

import lombok.Data;

@Data
public class InviteResponseRequest {
    private String callId;
    private String sdp;
}
