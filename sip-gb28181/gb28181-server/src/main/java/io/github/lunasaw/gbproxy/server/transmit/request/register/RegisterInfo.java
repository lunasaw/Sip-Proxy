package io.github.lunasaw.gbproxy.server.transmit.request.register;

import lombok.Data;

import java.util.Date;

/**
 * 设备注册信息，由 {@code ServerRegisterRequestProcessor} 在 REGISTER 成功后填充并随事件传递。
 *
 * @author luna
 * @date 2023/10/18
 */
@Data
public class RegisterInfo {

    /**
     * 注册时间
     */
    private Date registerTime;

    /**
     * 注册过期时间
     */
    private Integer expire;

    /**
     * 注册协议
     */
    private String transport;

    /**
     * 设备注册地址当前IP
     */
    private String localIp;

    /**
     * nat转换后看到的IP
     */
    private String remoteIp;

    /**
     * 经过rpotocol转换后的端口
     */
    private Integer remotePort;

    /**
     * 对端 GBT-28181 协议版本（{@code X-GB-Ver} 头域）。
     * 取值见 GBT-28181-2022 附录 I 表 I.1，例如 {@code 1.0/1.1/2.0/3.0}。
     * 缺失时为 {@code null}（对端为 2016 之前实现，未携带此扩展头）。
     */
    private String peerProtocolVersion;

    /**
     * GBT-28181-2022 §8.3：对端附带的 Note 摘要扩展头原始值。
     * 形如 {@code Digest nonce="<base64>", algorithm=SM3}。null 表示未携带。
     */
    private String peerNote;

    /**
     * GBT-28181-2022 §8.3：对端附带的 Monitor-User-Identity 跨域用户身份链。
     * 形如 {@code gw1-user001-attr1}。null 表示未携带（同域信令）。
     */
    private String peerMonitorUserIdentity;
}
