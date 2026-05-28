package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.sdp.TransportEnum;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.sdp.Rfc4566SdpParser;
import org.junit.jupiter.api.Test;

import javax.sdp.SdpParseException;

import static org.assertj.core.api.Assertions.assertThat;

class Gb28181SemanticInterpreterTest {

    private final Rfc4566SdpParser baseParser = new Rfc4566SdpParser();
    private final Gb28181SemanticInterpreter interpreter = new Gb28181SemanticInterpreter();

    @Test
    void interpret_playUdp_setsSessionTypeAndTransport() throws SdpParseException {
        GbSessionDescription gb = parseGb("v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n");

        interpreter.interpret(gb);

        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY);
        assertThat(gb.getTransport()).isEqualTo(TransportEnum.UDP);
        assertThat(gb.getAddress()).isEqualTo("192.168.1.1");
        assertThat(gb.getPort()).isEqualTo(9000);
    }

    @Test
    void interpret_playbackTcp_setsTransportTcp() throws SdpParseException {
        GbSessionDescription gb = parseGb("v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=PlayBack\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=1700000000 1700000600\r\n" +
                "m=video 6000 TCP/RTP/AVP 96\r\n");

        interpreter.interpret(gb);

        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY_BACK);
        assertThat(gb.getTransport()).isEqualTo(TransportEnum.TCP);
        assertThat(gb.getPort()).isEqualTo(6000);
    }

    @Test
    void interpret_download_resolvesEnum() throws SdpParseException {
        GbSessionDescription gb = parseGb("v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=Download\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 6000 RTP/AVP 96\r\n");

        interpreter.interpret(gb);

        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.DOWNLOAD);
    }

    @Test
    void interpret_unknownSessionName_leavesNull() throws SdpParseException {
        GbSessionDescription gb = parseGb("v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=Unknown\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 6000 RTP/AVP 96\r\n");

        interpreter.interpret(gb);

        assertThat(gb.getSessionType()).isNull();
    }

    @Test
    void interpret_nullBaseSdb_doesNotThrow() {
        GbSessionDescription gb = new GbSessionDescription(null);
        interpreter.interpret(gb);
        assertThat(gb.getSessionType()).isNull();
        assertThat(gb.getTransport()).isNull();
    }

    private GbSessionDescription parseGb(String sdp) throws SdpParseException {
        SdpSessionDescription base = baseParser.parse(sdp);
        return new GbSessionDescription(base.getBaseSdb());
    }
}
