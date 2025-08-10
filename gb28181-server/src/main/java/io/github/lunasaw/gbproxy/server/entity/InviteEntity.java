package io.github.lunasaw.gbproxy.server.entity;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.enums.ManufacturerEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.utils.SipUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author luna
 * @date 2023/11/6
 */
public class InviteEntity {

    public static void main(String[] args) {
        StringBuffer inviteBody = getInvitePlayBody(StreamModeEnum.UDP, "41010500002000000001", "127.0.0.1", 5060, "1234567890");
        System.out.println(inviteBody);
    }

    /**
     * 组装subject
     *
     * @param subId 通道Id
     * @param ssrc 混淆码
     * @param userId 设备Id
     * @return
     */
    public static String getSubject(String subId, String ssrc, String userId) {
        return String.format("%s:%s,%s:%s", subId, ssrc, userId, 0);
    }

    public static StringBuffer getInvitePlayBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc) {
        return getInvitePlayBody(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null);
    }

    public static StringBuffer getInvitePlayBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc,
        Boolean subStream, ManufacturerEnum manufacturerEnum) {
        return getInvitePlayBody(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturerEnum);
    }

    public static StringBuffer getInvitePlayBody(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort,
                                                 String ssrc) {
        return getInvitePlayBody(seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null);
    }

    public static StringBuffer getInvitePlayBody(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort,
                                                 String ssrc, Boolean subStream, ManufacturerEnum manufacturer) {

        return getInvitePlayBody(InviteSessionNameEnum.PLAY, seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturer, null,
                null);
    }

    public static StringBuffer getInvitePlayBackBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc,
                                                     String startTime, String endTime) {
        return getInvitePlayBodyBack(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null, startTime, endTime);
    }

    public static StringBuffer getInvitePlayBodyBack(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort,
                                                     String ssrc, Boolean subStream, ManufacturerEnum manufacturer, String startTime, String endTime) {

        return getInvitePlayBody(InviteSessionNameEnum.PLAY_BACK, seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturer,
                startTime, endTime);
    }

    /**
     *
     * @param seniorSdp [可选] 部分设备需要扩展SDP，需要打开此设置
     * @param streamModeEnum [必填] 流传输模式
     * @param userId [必填] 设备Id
     * @param sdpIp [必填] 设备IP
     * @param mediaPort [必填] 设备端口
     * @param ssrc [必填] 混淆码
     * @param subStream [可选] 是否子码流
     * @param manufacturer [可选] 设备厂商
     * @return
     */
    public static StringBuffer getInvitePlayBody(InviteSessionNameEnum inviteSessionNameEnum, Boolean seniorSdp, StreamModeEnum streamModeEnum,
                                                 String userId, String sdpIp, Integer mediaPort,
                                                 String ssrc, Boolean subStream, ManufacturerEnum manufacturer, String startTime, String endTime) {
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=").append(userId).append(" 0 0 IN IP4 ").append(sdpIp).append("\r\n");
        // Session Name
        content.append("s=").append(inviteSessionNameEnum.getType()).append("\r\n");
        content.append("u=").append(userId).append(":0\r\n");
        content.append("c=IN IP4 ").append(sdpIp).append("\r\n");
        if (InviteSessionNameEnum.PLAY_BACK.equals(inviteSessionNameEnum)) {
            // 将 ISO 8601 时间格式转换为 NTP 时间戳
            long startTimeNtp = SipUtils.toNtpTimestamp(startTime);
            long endTimeNtp = SipUtils.toNtpTimestamp(endTime);
            content.append("t=").append(startTimeNtp).append(" ").append(endTimeNtp).append("\r\n");
        } else {
            content.append("t=0 0\r\n");
        }

        if (seniorSdp) {
            if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if (StreamModeEnum.UDP.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 97 98 99\r\n");
            } else if (StreamModeEnum.UDP.equals(streamModeEnum)) {
                content.append("m=video ").append(mediaPort).append(" RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        addSsrc(content, ssrc);

        if (InviteSessionNameEnum.PLAY.equals(inviteSessionNameEnum)) {
            addSubStream(content, subStream, manufacturer);
        }

        return content;
    }

    public static StringBuffer addSsrc(StringBuffer content, String ssrc) {
        content.append("y=").append(ssrc).append("\r\n");// ssrc
        return content;
    }

    public static StringBuffer addSubStream(StringBuffer content, Boolean subStream, ManufacturerEnum manufacturer) {
        if (ManufacturerEnum.TP_LINK.equals(manufacturer)) {
            if (subStream) {
                content.append("a=streamMode:sub\r\n");
            } else {
                content.append("a=streamMode:main\r\n");
            }
        } else {
            if (subStream) {
                content.append("a=streamprofile:1\r\n");
            } else {
                content.append("a=streamprofile:0\r\n");
            }
        }
        return content;
    }

    // ======================== 以下是回放控制 ========================

    public static String playPause() {
        return playPause(null);
    }

    /**
     * 回放暂停
     *
     * @param cseq
     */
    public static String playPause(String cseq) {
        if (StringUtils.isBlank(cseq)) {
            cseq = String.valueOf(getInfoCseq());
        }
        StringBuilder content = new StringBuilder(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: ").append(cseq).append("\r\n");
        content.append("PauseTime: now\r\n");

        return content.toString();
    }

    public static String playNow() {
        return playNow(null);
    }

    /**
     * 回放恢复
     *
     * @param cseq
     * @return
     */
    public static String playNow(String cseq) {
        if (StringUtils.isBlank(cseq)) {
            cseq = String.valueOf(getInfoCseq());
        }
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: ").append(cseq).append("\r\n");
        content.append("Range: npt=now-\r\n");

        return content.toString();
    }

    public static String playRange(long seekTime) {
        return playRange(null, seekTime);
    }

    /**
     * 回放定位
     *
     * @param cseq
     * @param seekTime
     * @return
     */
    public static String playRange(String cseq, long seekTime) {
        if (StringUtils.isBlank(cseq)) {
            cseq = String.valueOf(getInfoCseq());
        }
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: ").append(cseq).append("\r\n");
        content.append("Range: npt=").append(Math.abs(seekTime)).append("-\r\n");

        return content.toString();
    }

    public static String playSpeed(Double speed) {
        return playSpeed(null, speed);
    }

    /**
     * 回放倍速
     *
     * @param cseq
     * @param speed
     * @return
     */
    public static String playSpeed(String cseq, Double speed) {
        if (StringUtils.isBlank(cseq)) {
            cseq = String.valueOf(getInfoCseq());
        }
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: ").append(cseq).append("\r\n");
        content.append("Scale: ").append(String.format("%.6f", speed)).append("\r\n");

        return content.toString();
    }

    private static int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }
}
