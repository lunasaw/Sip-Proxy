package io.github.lunasaw.sip.common.entity;

import javax.sdp.SessionDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDP解析器
 *
 * @author luna
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SdpSessionDescription {
    /**
     * 会话描述表示由会话描述协议（请参阅 IETF RFC 2327）定义的数据
     */
    private SessionDescription baseSdb;


    /**
     * 创建 SdpSessionDescription 实例。
     *
     * @param sdp JAIN-SDP 会话描述对象
     * @return SdpSessionDescription实例
     */
    public static SdpSessionDescription getInstance(SessionDescription sdp) {
        SdpSessionDescription sdpSessionDescription = new SdpSessionDescription();
        sdpSessionDescription.setBaseSdb(sdp);
        return sdpSessionDescription;
    }
}
