package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.enums.InviteSessionNameEnum;
import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.gb28181.common.entity.sdp.TransportEnum;
import io.github.lunasaw.sip.common.sdp.Rfc4566SdpParser;
import org.junit.jupiter.api.Test;

import javax.sdp.SdpParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gb28181SdpParser 集成测试。
 * <p>
 * 关键用例 {@link #parse_bugSampleFromIssue11_succeeds()} 锁定 2026-05-25 §1.1 故障路径——
 * 该样本未来必须永远能解析，否则 {@code InviteResponseProcessor} 又会发不出 ACK。
 */
class Gb28181SdpParserTest {

    private final Gb28181SdpParser parser = new Gb28181SdpParser(
            new Rfc4566SdpParser(),
            List.of(new YFieldParser(), new FFieldParser()),
            new Gb28181SemanticInterpreter()
    );

    /** doc/SDP-PARSER-LAYERING-PLAN.md §1.1 触发 bug 的真实 200 OK SDP 样本。 */
    @Test
    void parse_bugSampleFromIssue11_succeeds() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=34020000001320000001 0 0 IN IP4 127.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 10000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "y=0100000001\r\n";

        GbSessionDescription gb = parser.parse(sdp);

        assertThat(gb).isNotNull();
        assertThat(gb.getBaseSdb()).isNotNull();
        assertThat(gb.getSsrc()).isEqualTo("0100000001");
        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY);
        assertThat(gb.getTransport()).isEqualTo(TransportEnum.UDP);
        assertThat(gb.getAddress()).isEqualTo("127.0.0.1");
        assertThat(gb.getPort()).isEqualTo(10000);
    }

    @Test
    void parse_withFAndY_extractsBothExtensionLines() throws SdpParseException {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=PlayBack\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=1700000000 1700000600\r\n" +
                "m=video 6000 TCP/RTP/AVP 96\r\n" +
                "y=1234567890\r\n" +
                "f=v/2/5/25/1/4000a/1/8/1\r\n";

        GbSessionDescription gb = parser.parse(sdp);

        assertThat(gb.getSsrc()).isEqualTo("1234567890");
        assertThat(gb.getMediaParam()).isEqualTo("v/2/5/25/1/4000a/1/8/1");
        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY_BACK);
        assertThat(gb.getTransport()).isEqualTo(TransportEnum.TCP);
    }

    @Test
    void parse_withoutGbExtensions_stillReturnsGbModel() throws SdpParseException {
        // 纯标准 SDP 也必须能解析（无 y= / f=）
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n";

        GbSessionDescription gb = parser.parse(sdp);

        assertThat(gb.getSsrc()).isNull();
        assertThat(gb.getMediaParam()).isNull();
        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY);
        assertThat(gb.getPort()).isEqualTo(9000);
    }

    @Test
    void parse_unparseableBaseSdp_throws() {
        // 标准字段语法错误时仍按 RFC 4566 抛 SdpParseException
        // 这里用一个 m= 字段端口非数字的明确语法错误（JAIN-SDP 严格校验）
        String badSdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video NOT_A_PORT RTP/AVP 96\r\n";
        assertThatThrownBy(() -> parser.parse(badSdp))
                .isInstanceOf(SdpParseException.class);
    }

    @Test
    void parse_extensionParserThrows_doesNotFailWholeParse() throws SdpParseException {
        // 软失败原则：单条扩展行解析异常不应让整个 SDP 解析失败
        GbSdpExtensionParser flaky = new GbSdpExtensionParser() {
            @Override
            public boolean accepts(String line) { return line.startsWith("y="); }

            @Override
            public void apply(String line, GbSessionDescription target) { throw new RuntimeException("simulated"); }

            @Override
            public boolean stripBeforeBaseParse() { return true; }
        };
        Gb28181SdpParser flakyParser = new Gb28181SdpParser(
                new Rfc4566SdpParser(),
                List.of(flaky),
                new Gb28181SemanticInterpreter()
        );

        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 10.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 10.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n" +
                "y=0100000001\r\n";

        GbSessionDescription gb = flakyParser.parse(sdp);

        assertThat(gb).isNotNull();
        assertThat(gb.getSsrc()).isNull();           // 失败的扩展未写入
        assertThat(gb.getSessionType()).isEqualTo(InviteSessionNameEnum.PLAY); // 标准字段仍正确
    }
}
