package io.github.lunasaw.gbproxy.server.entity;

import io.github.lunasaw.gb28181.common.entity.enums.ManufacturerEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gb28181.common.entity.utils.GbUtil;
import lombok.Data;

/**
 * INVITE 请求参数封装，持有 SDP 构建所需的全部字段，并提供各场景的 SDP 内容生成方法。
 *
 * @author luna
 * @date 2023/11/16
 */
@Data
public class InviteRequest {

    /**
     * 是否高级sdp
     */
    private Boolean          seniorSdp;
    /**
     * 流媒体传输模式
     */
    private StreamModeEnum   streamModeEnum;
    /**
     * 用户ID
     */
    private String           userId;
    /**
     * 收流IP
     */
    private String           sdpIp;
    /**
     * 收流端口
     */
    private Integer          mediaPort;
    /**
     * ssrc
     */
    private String           ssrc;
    /**
     * 是否订阅子码流
     */
    private Boolean          subStream;
    /**
     * 厂商
     */
    private ManufacturerEnum manufacturer;
    /**
     * 回放开始时间
     */
    private String           startTime;
    /**
     * 回放结束时间
     */
    private String           endTime;

    public InviteRequest(String userId, StreamModeEnum streamModeEnum, String sdpIp, Integer mediaPort) {
        this.seniorSdp = false;
        this.streamModeEnum = streamModeEnum;
        this.userId = userId;
        this.sdpIp = sdpIp;
        this.mediaPort = mediaPort;
        this.ssrc = GbUtil.genSsrc(userId);
    }

    public InviteRequest(Boolean seniorSdp, StreamModeEnum streamModeEnum, String userId, String sdpIp, Integer mediaPort, String ssrc) {
        this.seniorSdp = seniorSdp;
        this.streamModeEnum = streamModeEnum;
        this.userId = userId;
        this.sdpIp = sdpIp;
        this.mediaPort = mediaPort;
        this.ssrc = ssrc;
    }

    public InviteRequest(String userId, StreamModeEnum streamModeEnum, String sdpIp, Integer mediaPort, String startTime, String endTime) {
        this.seniorSdp = false;
        this.streamModeEnum = streamModeEnum;
        this.userId = userId;
        this.sdpIp = sdpIp;
        this.mediaPort = mediaPort;
        this.ssrc = GbUtil.genSsrc(userId);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 获取实时点播 SDP 内容。
     *
     * @return SDP 字符串
     */
    public String getContent() {
        if (seniorSdp == null || !seniorSdp) {
            return InviteEntity.getInvitePlayBody(streamModeEnum, userId, sdpIp, mediaPort, ssrc).toString();
        }
        return getContentWithSdp();
    }

    /**
     * 获取历史回放 SDP 内容。
     *
     * @return SDP 字符串
     */
    public String getBackContent() {
        return InviteEntity.getInvitePlayBackBody(streamModeEnum, userId, sdpIp, mediaPort, ssrc, startTime, endTime).toString();
    }

    /**
     * 获取带子码流扩展的实时点播 SDP 内容。
     *
     * @return SDP 字符串
     */
    public String getContentWithSub() {
        return InviteEntity.getInvitePlayBody(streamModeEnum, userId, sdpIp, mediaPort, ssrc, subStream, manufacturer).toString();
    }

    /**
     * 获取带高级 SDP 的实时点播 SDP 内容。
     *
     * @return SDP 字符串
     */
    public String getContentWithSdp() {
        return InviteEntity.getInvitePlayBody(seniorSdp, streamModeEnum, userId, sdpIp, mediaPort, ssrc).toString();
    }

    /**
     * 获取 SDP Subject 字段。
     *
     * @param currentUserId 平台侧用户 ID
     * @return Subject 字符串
     */
    public String getSubject(String currentUserId) {
        return InviteEntity.getSubject(userId, ssrc, currentUserId);
    }
}
