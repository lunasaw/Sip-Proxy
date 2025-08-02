package io.github.lunasaw.gbproxy.server.transmit.request.invite;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务端INVITE请求业务处理器默认实现
 * 提供默认的业务逻辑处理实现
 *
 * @author luna
 */
@Slf4j
@Component
public class DefaultServerInviteRequestHandler implements ServerInviteRequestHandler {

    @Override
    public void inviteSession(String callId, SdpSessionDescription sessionDescription) {
        log.info("📺 服务端处理INVITE会话: callId={}, sessionDescription={}", callId, sessionDescription);
        // 默认实现：记录日志，实际业务逻辑由具体实现类处理
    }

    @Override
    public String getInviteResponse(String userId, SdpSessionDescription sessionDescription) {
        log.info("📺 服务端生成INVITE响应: userId={}, sessionDescription={}", userId, sessionDescription);
        // 默认实现：返回空的SDP内容，实际业务逻辑由具体实现类处理
        return "";
    }
}