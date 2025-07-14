package io.github.lunasaw.gbproxy.client.transmit.request.invite;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;

/**
 * @author luna
 * @date 2025/7/13
 */
public class DefaultInviteRequestHandler implements InviteRequestHandler {
    @Override
    public void inviteSession(String callId, SdpSessionDescription sessionDescription) {

    }

    @Override
    public String getInviteResponse(String userId, SdpSessionDescription sessionDescription) {
        return "";
    }
}
