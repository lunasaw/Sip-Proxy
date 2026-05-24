package io.github.lunasaw.gb28181.common.entity.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GbUtilTest {

    @Test
    void generateGB28181Code_formatsCorrectly() {
        String code = GbUtil.generateGB28181Code(34020000, 13, 200, 1);
        assertThat(code).hasSize(20);
        assertThat(code).startsWith("34020000");
        assertThat(code).contains("13");
        assertThat(code).endsWith("0000001");
    }

    @Test
    void genSsrc_withUserId_usesPrefixAndCorrectLength() {
        String ssrc = GbUtil.genSsrc("34020000001320000001");
        assertThat(ssrc).hasSize(9);
        assertThat(ssrc).startsWith("20000");
    }

    @Test
    void genSsrc_withNull_returnsNumericString() {
        String ssrc = GbUtil.genSsrc(null);
        assertThat(ssrc).matches("\\d+");
    }

    @Test
    void genSsrc_withEmpty_returnsNumericString() {
        String ssrc = GbUtil.genSsrc("");
        assertThat(ssrc).matches("\\d+");
    }
}
