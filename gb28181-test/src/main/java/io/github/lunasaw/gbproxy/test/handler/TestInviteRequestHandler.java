package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.client.transmit.request.invite.InviteRequestHandler;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class TestInviteRequestHandler implements InviteRequestHandler {

    private static final String TEST_SDP =
        "v=0\r\n" +
        "o=34020000001320000001 0 0 IN IP4 127.0.0.1\r\n" +
        "s=Play\r\n" +
        "c=IN IP4 127.0.0.1\r\n" +
        "t=0 0\r\n" +
        "m=video 10000 RTP/AVP 96\r\n" +
        "a=rtpmap:96 PS/90000\r\n" +
        "y=0100000001\r\n";

    @Override
    public void inviteSession(String callId, SdpSessionDescription sessionDescription) {
    }

    @Override
    public String getInviteResponse(String userId, SdpSessionDescription sessionDescription) {
        return TEST_SDP;
    }
}
