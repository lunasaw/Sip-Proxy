package io.github.lunasaw.sip.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 远端地址信息，封装IP和端口。
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RemoteAddressInfo {
    /** 远端IP地址。 */
    private String ip;
    /** 远端端口号。 */
    private Integer port;

}
