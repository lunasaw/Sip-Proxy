package io.github.lunasaw.gbproxy.client.transmit.request.invite;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;

/**
 * INVITE请求业务处理器接口
 * 负责处理INVITE请求的业务逻辑
 *
 * @author weidian
 */
public interface InviteRequestHandler {

    /**
     * 处理INVITE会话
     *
     * @param callId             呼叫ID
     * @param sessionDescription 会话描述
     */
    void inviteSession(String callId, SdpSessionDescription sessionDescription);

    /**
     * 获取INVITE响应内容
     *
     * @param userId             用户ID
     * @param sessionDescription 会话描述
     * @return 响应内容
     */
    String getInviteResponse(String userId, SdpSessionDescription sessionDescription);
}