package io.github.lunasaw.gb28181.common.entity.mansrtsp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 附录 B MANSRTSP 解析测试。
 */
class ManSrtspParserTest {

    @Test
    void parse_playWithRange_shouldExtractFields() {
        String text = "PLAY MANSRTSP/1.0\r\nCSeq: 1\r\nRange: npt=now-\r\n";
        ManSrtspRequest req = ManSrtspParser.parse(text);
        assertThat(req.getMethod()).isEqualTo("PLAY");
        assertThat(req.getVersion()).isEqualTo("MANSRTSP/1.0");
        assertThat(req.getCSeq()).isEqualTo(1);
        assertThat(req.getRange()).isEqualTo("npt=now-");
        assertThat(req.getScale()).isNull();
    }

    @Test
    void parse_pauseWithPauseTime_shouldExtractFields() {
        String text = "PAUSE MANSRTSP/1.0\r\nCSeq: 2\r\nPauseTime: now\r\n";
        ManSrtspRequest req = ManSrtspParser.parse(text);
        assertThat(req.getMethod()).isEqualTo("PAUSE");
        assertThat(req.getCSeq()).isEqualTo(2);
        assertThat(req.getPauseTime()).isEqualTo("now");
    }

    @Test
    void parse_playWithScale_shouldExtractFields() {
        String text = "PLAY MANSRTSP/1.0\r\nCSeq: 3\r\nScale: 2.0\r\n";
        ManSrtspRequest req = ManSrtspParser.parse(text);
        assertThat(req.getMethod()).isEqualTo("PLAY");
        assertThat(req.getCSeq()).isEqualTo(3);
        assertThat(req.getScale()).isEqualTo(2.0);
    }

    @Test
    void parse_teardown_shouldHaveOnlyMethodAndCSeq() {
        String text = "TEARDOWN MANSRTSP/1.0\r\nCSeq: 4\r\n";
        ManSrtspRequest req = ManSrtspParser.parse(text);
        assertThat(req.getMethod()).isEqualTo("TEARDOWN");
        assertThat(req.getCSeq()).isEqualTo(4);
        assertThat(req.getRange()).isNull();
        assertThat(req.getScale()).isNull();
    }

    @Test
    void parse_emptyOrNull_shouldReturnRawOnly() {
        ManSrtspRequest empty = ManSrtspParser.parse("");
        assertThat(empty.getMethod()).isNull();
        assertThat(empty.getRaw()).isEmpty();

        ManSrtspRequest nullReq = ManSrtspParser.parse(null);
        assertThat(nullReq.getRaw()).isNull();
    }

    @Test
    void parse_withRawPreserved() {
        String text = "PLAY MANSRTSP/1.0\r\nCSeq: 1\r\n";
        ManSrtspRequest req = ManSrtspParser.parse(text);
        assertThat(req.getRaw()).isEqualTo(text);
    }
}
