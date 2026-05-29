package io.github.lunasaw.gb28181.common.entity.sdp;

import javax.sdp.SessionDescription;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GB/T 28181 SDP 扩展模型。
 * <p>
 * 包含三类字段：
 * <ul>
 *   <li>顶级扩展行（{@code y=}、{@code f=}）— 由 {@link io.github.lunasaw.gb28181.common.sdp.GbSdpExtensionParser} 写入</li>
 *   <li>标准字段 GB 语义化结果（{@code s=} → sessionType、{@code m=} → transport、{@code c=} → address/port）— 由
 *       {@link io.github.lunasaw.gb28181.common.sdp.Gb28181SemanticInterpreter} 填充</li>
 *   <li>GB 私有 {@code a=} 属性族（阶段二补充）</li>
 * </ul>
 *
 * @author luna
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GbSessionDescription extends SdpSessionDescription {

    /** 顶级扩展行 y= 取值（SSRC，附录 G "y 字段"） */
    private String ssrc;

    /** 顶级扩展行 f= 取值（媒体参数原文，附录 G "f 字段"；阶段二补充内部 9 段结构化解析） */
    private String mediaParam;

    /** s= 字段语义（Play / PlayBack / Talk / Download，附录 G "s 字段"） */
    private InviteSessionNameEnum sessionType;

    /** m= 字段传输方式（UDP / TCP，附录 G "m 字段"） */
    private TransportEnum transport;

    /** c= 字段连接地址（冗余便利字段） */
    private String address;

    /** m= 字段端口（冗余便利字段） */
    private Integer port;


    public GbSessionDescription(SessionDescription sessionDescription) {
        super(sessionDescription);
    }
}
