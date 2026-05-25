package io.github.lunasaw.sip.common.sdp;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import org.junit.jupiter.api.Test;

import javax.sdp.SdpParseException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rfc4566SdpParserTest {

    private final Rfc4566SdpParser parser = new Rfc4566SdpParser();

    @Test
    void parse_standardSdp_extractsOriginAndConnection() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=34020000001320000001 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n";

        SdpSessionDescription result = parser.parse(sdp, StandardCharsets.UTF_8);

        assertThat(result).isNotNull();
        assertThat(result.getBaseSdb()).isNotNull();
        assertThat(result.getBaseSdb().getOrigin().getUsername()).isEqualTo("34020000001320000001");
        assertThat(result.getBaseSdb().getOrigin().getAddress()).isEqualTo("192.168.1.1");
        assertThat(result.getBaseSdb().getConnection().getAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void parse_defaultCharsetOverload_equivalentToUtf8() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 127.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n";

        SdpSessionDescription result = parser.parse(sdp);

        assertThat(result.getBaseSdb().getOrigin().getAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void parse_yFieldUnstripped_throws() {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 127.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 10000 RTP/AVP 96\r\n" +
                "y=0100000001\r\n";

        assertThatThrownBy(() -> parser.parse(sdp, StandardCharsets.UTF_8))
                .isInstanceOf(SdpParseException.class);
    }
}
