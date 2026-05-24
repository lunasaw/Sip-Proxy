package io.github.lunasaw.gb28181.common.entity.sdp;

import javax.sdp.SessionDescription;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GB28181 SDP 扩展模型，包含标准 SDP 之外的 ssrc / mediaDescription 字段。
 *
 * @author luna
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GbSessionDescription extends SdpSessionDescription {

    private String ssrc;

    private String mediaDescription;

    /**
     * 冗余处理
     */
    private String address;

    private Integer port;


    public GbSessionDescription(SessionDescription sessionDescription) {
        super(sessionDescription);
    }

    public static GbSessionDescription getInstance(SessionDescription sessionDescription, String ssrc, String mediaDescription) {
        GbSessionDescription gbSessionDescription = new GbSessionDescription(sessionDescription);
        gbSessionDescription.setSsrc(ssrc);
        gbSessionDescription.setMediaDescription(mediaDescription);
        return gbSessionDescription;
    }

}
