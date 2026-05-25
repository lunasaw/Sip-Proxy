package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YFieldParserTest {

    private final YFieldParser parser = new YFieldParser();

    @Test
    void accepts_yEquals_returnsTrue() {
        assertThat(parser.accepts("y=0100000001")).isTrue();
    }

    @Test
    void accepts_otherLine_returnsFalse() {
        assertThat(parser.accepts("a=rtpmap:96 PS/90000")).isFalse();
        assertThat(parser.accepts("f=v/2/4")).isFalse();
        assertThat(parser.accepts("")).isFalse();
    }

    @Test
    void stripBeforeBaseParse_returnsTrue() {
        assertThat(parser.stripBeforeBaseParse()).isTrue();
    }

    @Test
    void apply_validSsrc_writesToTarget() {
        GbSessionDescription target = new GbSessionDescription(null);
        parser.apply("y=0100000001", target);
        assertThat(target.getSsrc()).isEqualTo("0100000001");
    }

    @Test
    void apply_emptyValue_writesEmptyString() {
        GbSessionDescription target = new GbSessionDescription(null);
        parser.apply("y=", target);
        assertThat(target.getSsrc()).isEmpty();
    }
}
