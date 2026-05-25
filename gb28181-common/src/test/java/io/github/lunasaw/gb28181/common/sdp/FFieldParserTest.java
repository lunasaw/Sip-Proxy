package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FFieldParserTest {

    private final FFieldParser parser = new FFieldParser();

    @Test
    void accepts_fEquals_returnsTrue() {
        assertThat(parser.accepts("f=v/2/5/25/1/4000a/1/8/1")).isTrue();
        assertThat(parser.accepts("f=")).isTrue();
    }

    @Test
    void accepts_otherLine_returnsFalse() {
        assertThat(parser.accepts("y=0100000001")).isFalse();
        assertThat(parser.accepts("a=rtpmap:96 PS/90000")).isFalse();
    }

    @Test
    void stripBeforeBaseParse_returnsTrue() {
        assertThat(parser.stripBeforeBaseParse()).isTrue();
    }

    @Test
    void apply_validF_writesRawString() {
        GbSessionDescription target = new GbSessionDescription(null);
        parser.apply("f=v/2/5/25/1/4000a/1/8/1", target);
        assertThat(target.getMediaParam()).isEqualTo("v/2/5/25/1/4000a/1/8/1");
    }

    @Test
    void apply_emptyF_writesEmpty() {
        GbSessionDescription target = new GbSessionDescription(null);
        parser.apply("f=", target);
        assertThat(target.getMediaParam()).isEmpty();
    }
}
