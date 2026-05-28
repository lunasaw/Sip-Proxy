package io.github.lunasaw.gbproxy.server.entity;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.enums.ManufacturerEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.utils.SipUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * GB28181 INVITE SDP body 构建工具类，提供实时点播、历史回放、语音对讲、文件下载及回放控制的 SDP 内容生成。
 *
 * @author luna
 * @date 2023/11/6
 */
public class InviteEntity {

    public static void main(String[] args) {
        StringBuffer inviteBody = getInvitePlayBody(StreamModeEnum.UDP, "41010500002000000001", "127.0.0.1", 5060, "1234567890");
        System.out.println(inviteBody);
    }

    /**
     * 组装 SDP Subject 字段。
     *
     * @param subId  通道 ID
     * @param ssrc   SSRC 混淆码
     * @param userId 设备 ID
     * @return Subject 字符串，格式为 {@code subId:ssrc,userId:0}
     */
    public static String getSubject(String subId, String ssrc, String userId) {
        return String.format("%s:%s,%s:%s", subId, ssrc, userId, 0);
    }

    /**
     * 构建实时点播 SDP body（简化重载，不使用高级 SDP 和子码流）。
     *
     * @param streamModeEnum 流传输模式
     * @param userId         设备 ID
     * @param sdpIp          收流 IP
     * @param mediaPort      收流端口
     * @param ssrc           SSRC 混淆码
     * @return SDP 内容
     */
    public static StringBuffer getInvitePlayBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc) {
        return getInvitePlayBody(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null);
    }

    /**
     * 构建实时点播 SDP body（支持子码流和厂商扩展）。
     *
     * @param streamModeEnum   流传输模式
     * @param userId           设备 ID
     * @param sdpIp            收流 IP
     * @param mediaPort        收流端口
     * @param ssrc             SSRC 混淆码
     * @param subStream        是否子码流
     * @param manufacturerEnum 设备厂商（影响子码流扩展字段格式）
     * @return SDP 内容
     */
    public static StringBuffer getInvitePlayBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc,
        Boolean subStream, ManufacturerEnum manufacturerEnum) {
        return getInvitePlayBody(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturerEnum);
    }

    /**
     * 构建实时点播 SDP body（支持高级 SDP 模式）。
     *
     * @param seniorSdp      是否使用高级 SDP（部分设备需要扩展编码列表）
     * @param streamModeEnum 流传输模式
     * @param userId         设备 ID
     * @param sdpIp          收流 IP
     * @param mediaPort      收流端口
     * @param ssrc           SSRC 混淆码
     * @return SDP 内容
     */
    public static StringBuffer getInvitePlayBody(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort,
                                                 String ssrc) {
        return getInvitePlayBody(seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null);
    }

    public static StringBuffer getInvitePlayBody(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort,
                                                 String ssrc, Boolean subStream, ManufacturerEnum manufacturer) {

        return getInvitePlayBody(InviteSessionNameEnum.PLAY, seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturer, null,
                null);
    }

    /**
     * 构建历史回放 SDP body（简化重载）。
     *
     * @param streamModeEnum 流传输模式
     * @param userId         设备 ID
     * @param sdpIp          收流 IP
     * @param mediaPort      收流端口
     * @param ssrc           SSRC 混淆码
     * @param startTime      回放起始时间（ISO 8601）
     * @param endTime        回放结束时间（ISO 8601）
     * @return SDP 内容
     */
    public static StringBuffer getInvitePlayBackBody(StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc,
                                                     String startTime, String endTime) {
        return getInvitePlayBodyBack(false, streamModeEnum, userId, sdpIp, mediaPort, ssrc, false, null, startTime, endTime);
    }

    /**
     * 构建历史回放 SDP body（完整参数）。
     *
     * @param seniorSdp      是否使用高级 SDP
     * @param streamModeEnum 流传输模式
     * @param userId         设备 ID
     * @param sdpIp          收流 IP
     * @param mediaPort      收流端口
     * @param ssrc           SSRC 混淆码
     * @param subStream      是否子码流
     * @param manufacturer   设备厂商
     * @param startTime      回放起始时间（ISO 8601）
     * @param endTime        回放结束时间（ISO 8601）
     * @return SDP 内容
     */
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
    /**
     * 构建通用 INVITE SDP body（实时点播或历史回放，由 inviteSessionNameEnum 区分）。
     *
     * @param inviteSessionNameEnum 会话类型（PLAY/PLAY_BACK）
     * @param seniorSdp             是否使用高级 SDP
     * @param streamModeEnum        流传输模式
     * @param userId                设备 ID
     * @param sdpIp                 收流 IP
     * @param mediaPort             收流端口
     * @param ssrc                  SSRC 混淆码
     * @param subStream             是否子码流
     * @param manufacturer          设备厂商
     * @param startTime             回放起始时间（ISO 8601，仅 PLAY_BACK 时有效）
     * @param endTime               回放结束时间（ISO 8601，仅 PLAY_BACK 时有效）
     * @return SDP 内容
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

    /**
     * 向 SDP 内容追加 SSRC 行（y=ssrc）。
     *
     * @param content SDP 内容缓冲区
     * @param ssrc    SSRC 混淆码
     * @return 追加后的缓冲区
     */
    public static StringBuffer addSsrc(StringBuffer content, String ssrc) {
        content.append("y=").append(ssrc).append("\r\n");// ssrc
        return content;
    }

    /**
     * GB28181-2022 §9.12.2 语音对讲 SDP body (audio-only, sendonly)
     *
     * 复用 PLAY 的 INVITE 流程，仅替换 m= 为 audio + 编码 PCMA(8) + sendonly。
     */
    public static StringBuffer getInviteTalkBody(StreamModeEnum streamModeEnum, String userId,
                                                  String sdpIp, Integer mediaPort, String ssrc) {
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=").append(userId).append(" 0 0 IN IP4 ").append(sdpIp).append("\r\n");
        content.append("s=").append(InviteSessionNameEnum.TALK.getType()).append("\r\n");
        content.append("u=").append(userId).append(":0\r\n");
        content.append("c=IN IP4 ").append(sdpIp).append("\r\n");
        content.append("t=0 0\r\n");
        if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
            content.append("m=audio ").append(mediaPort).append(" TCP/RTP/AVP 8\r\n");
        } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
            content.append("m=audio ").append(mediaPort).append(" TCP/RTP/AVP 8\r\n");
        } else {
            content.append("m=audio ").append(mediaPort).append(" RTP/AVP 8\r\n");
        }
        content.append("a=sendonly\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");
        if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
            content.append("a=setup:passive\r\n");
            content.append("a=connection:new\r\n");
        } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
            content.append("a=setup:active\r\n");
            content.append("a=connection:new\r\n");
        }
        // 媒体格式描述: f=v/////a/1/8/1（无视频；音频PCMA/8000Hz/单声道）
        content.append("f=v/////a/1/8/1\r\n");
        addSsrc(content, ssrc);
        return content;
    }

    /**
     * GB28181-2022 §9.9 视音频文件下载 SDP body
     *
     * @param downloadSpeed 下载倍速 (1/2/4/8 等，可选)
     */
    public static StringBuffer getInviteDownloadBody(StreamModeEnum streamModeEnum, String userId,
                                                      String sdpIp, Integer mediaPort, String ssrc,
                                                      String startTime, String endTime, Integer downloadSpeed) {
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=").append(userId).append(" 0 0 IN IP4 ").append(sdpIp).append("\r\n");
        content.append("s=").append(InviteSessionNameEnum.DOWNLOAD.getType()).append("\r\n");
        content.append("u=").append(userId).append(":3\r\n");
        content.append("c=IN IP4 ").append(sdpIp).append("\r\n");
        long startNtp = io.github.lunasaw.sip.common.utils.SipUtils.toNtpTimestamp(startTime);
        long endNtp = io.github.lunasaw.sip.common.utils.SipUtils.toNtpTimestamp(endTime);
        content.append("t=").append(startNtp).append(" ").append(endNtp).append("\r\n");
        if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
            content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 97 98 99\r\n");
        } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
            content.append("m=video ").append(mediaPort).append(" TCP/RTP/AVP 96 97 98 99\r\n");
        } else {
            content.append("m=video ").append(mediaPort).append(" RTP/AVP 96 97 98 99\r\n");
        }
        content.append("a=recvonly\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        content.append("a=rtpmap:97 MPEG4/90000\r\n");
        content.append("a=rtpmap:98 H264/90000\r\n");
        content.append("a=rtpmap:99 H265/90000\r\n");
        if (downloadSpeed != null && downloadSpeed > 0) {
            content.append("a=downloadspeed:").append(downloadSpeed).append("\r\n");
        }
        if (StreamModeEnum.TCP_PASSIVE.equals(streamModeEnum)) {
            content.append("a=setup:passive\r\n");
            content.append("a=connection:new\r\n");
        } else if (StreamModeEnum.TCP_ACTIVE.equals(streamModeEnum)) {
            content.append("a=setup:active\r\n");
            content.append("a=connection:new\r\n");
        }
        addSsrc(content, ssrc);
        return content;
    }

    /**
     * 向 SDP 内容追加子码流扩展行（a=streamMode 或 a=streamprofile）。
     *
     * @param content      SDP 内容缓冲区
     * @param subStream    是否子码流
     * @param manufacturer 设备厂商（影响扩展字段格式）
     * @return 追加后的缓冲区
     */
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

    /**
     * 回放暂停（不带 CSeq，自动生成）。
     *
     * @return RTSP 控制消息字符串
     */
    public static String playPause() {
        return playPause(null);
    }

    /**
     * 回放暂停（指定 CSeq）。
     *
     * @param cseq CSeq 序号，为空时自动生成
     * @return RTSP 控制消息字符串
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

    /**
     * 回放恢复（不带 CSeq，自动生成）。
     *
     * @return RTSP 控制消息字符串
     */
    public static String playNow() {
        return playNow(null);
    }

    /**
     * 回放恢复（指定 CSeq）。
     *
     * @param cseq CSeq 序号，为空时自动生成
     * @return RTSP 控制消息字符串
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

    /**
     * 回放定位（不带 CSeq，自动生成）。
     *
     * @param seekTime 定位时间（秒）
     * @return RTSP 控制消息字符串
     */
    public static String playRange(long seekTime) {
        return playRange(null, seekTime);
    }

    /**
     * 回放定位（指定 CSeq）。
     *
     * @param cseq     CSeq 序号，为空时自动生成
     * @param seekTime 定位时间（秒）
     * @return RTSP 控制消息字符串
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

    /**
     * 回放倍速（不带 CSeq，自动生成）。
     *
     * @param speed 倍速值（如 2.0 表示 2 倍速）
     * @return RTSP 控制消息字符串
     */
    public static String playSpeed(Double speed) {
        return playSpeed(null, speed);
    }

    /**
     * 回放倍速（指定 CSeq）。
     *
     * @param cseq  CSeq 序号，为空时自动生成
     * @param speed 倍速值
     * @return RTSP 控制消息字符串
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
