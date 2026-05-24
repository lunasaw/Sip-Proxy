package io.github.lunasaw.gb28181.common.entity.utils;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import org.junit.jupiter.api.Test;

import javax.sdp.SdpParseException;

import static org.assertj.core.api.Assertions.assertThat;

class GbSdpUtilsTest {

    @Test
    void parseGbSdp_withYandFFields_extractsCorrectly() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n" +
                "y=0100000001\r\n" +
                "f=v/2/4\r\n";
        GbSessionDescription result = GbSdpUtils.parseGbSdp(sdp);
        assertThat(result.getSsrc()).isEqualTo("0100000001");
        assertThat(result.getMediaDescription()).isEqualTo("v/2/4");
        assertThat(result.getBaseSdb()).isNotNull();
    }

    @Test
    void parseGbSdp_withoutYandF_returnsNullSsrc() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n";
        GbSessionDescription result = GbSdpUtils.parseGbSdp(sdp);
        assertThat(result.getSsrc()).isNull();
        assertThat(result.getMediaDescription()).isNull();
        assertThat(result.getBaseSdb()).isNotNull();
    }
}
