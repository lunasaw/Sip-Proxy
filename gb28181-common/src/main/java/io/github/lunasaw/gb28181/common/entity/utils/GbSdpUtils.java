package io.github.lunasaw.gb28181.common.entity.utils;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.utils.SipUtils;

/**
 * GB28181 SDP 解析工具：
 * jainSip 不支持 y= / f= 字段，先剥离这两个 GB 扩展字段，
 * 再交由 sip-common 的标准解析器处理剩余部分，最后组合回 {@link GbSessionDescription}。
 *
 * @author luna
 */
public class GbSdpUtils {

    private GbSdpUtils() {}

    public static GbSessionDescription parseGbSdp(String sdpStr) {
        int ssrcIndex = sdpStr.indexOf("y=");
        int mediaDescriptionIndex = sdpStr.indexOf("f=");

        SdpSessionDescription base;
        String ssrc = null;
        String mediaDescription = null;

        if (mediaDescriptionIndex == 0 && ssrcIndex == 0) {
            base = SipUtils.parseSdp(sdpStr);
        } else {
            String[] lines = sdpStr.split("\\r?\\n");
            StringBuilder stringBuilder = new StringBuilder();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("y=")) {
                    ssrc = line.substring(2);
                } else if (trimmed.startsWith("f=")) {
                    mediaDescription = line.substring(2);
                } else {
                    stringBuilder.append(trimmed).append("\r\n");
                }
            }
            base = SipUtils.parseSdp(stringBuilder.toString());
        }
        return GbSessionDescription.getInstance(base.getBaseSdb(), ssrc, mediaDescription);
    }
}
