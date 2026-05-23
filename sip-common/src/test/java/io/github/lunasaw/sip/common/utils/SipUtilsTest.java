package io.github.lunasaw.sip.common.utils;

import io.github.lunasaw.sip.common.entity.GbSessionDescription;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import org.junit.jupiter.api.Test;

import javax.sip.header.SubjectHeader;
import javax.sip.message.Request;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SipUtilsTest {

    @Test
    void generateGB28181Code_formatsCorrectly() {
        String code = SipUtils.generateGB28181Code(34020000, 13, 200, 1);
        assertThat(code).hasSize(20);
        assertThat(code).startsWith("34020000");
        assertThat(code).contains("13");
        assertThat(code).endsWith("0000001");
    }

    @Test
    void genSsrc_withUserId_usesPrefixAndCorrectLength() {
        String ssrc = SipUtils.genSsrc("34020000001320000001");
        assertThat(ssrc).hasSize(9);
        assertThat(ssrc).startsWith("20000");
    }

    @Test
    void genSsrc_withNull_returnsNumericString() {
        String ssrc = SipUtils.genSsrc(null);
        assertThat(ssrc).matches("\\d+");
    }

    @Test
    void genSsrc_withEmpty_returnsNumericString() {
        String ssrc = SipUtils.genSsrc("");
        assertThat(ssrc).matches("\\d+");
    }

    @Test
    void toNtpTimestamp_epoch_returnsOffset() {
        long ntp = SipUtils.toNtpTimestamp(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
        assertThat(ntp).isEqualTo(2208988800L);
    }

    @Test
    void toNtpTimestamp_null_returnsZero() {
        assertThat(SipUtils.toNtpTimestamp((LocalDateTime) null)).isEqualTo(0);
    }

    @Test
    void toNtpTimestamp_validString_returnsPositive() {
        long ntp = SipUtils.toNtpTimestamp("2024-01-01T08:00:00");
        assertThat(ntp).isGreaterThan(2208988800L);
    }

    @Test
    void toNtpTimestamp_invalidString_returnsZero() {
        assertThat(SipUtils.toNtpTimestamp("not-a-date")).isEqualTo(0);
    }

    @Test
    void toNtpTimestamp_nullString_returnsZero() {
        assertThat(SipUtils.toNtpTimestamp((String) null)).isEqualTo(0);
    }

    @Test
    void parseSdp_withYandFFields_extractsCorrectly() {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n" +
                "y=0100000001\r\n" +
                "f=v/2/4\r\n";
        GbSessionDescription result = (GbSessionDescription) SipUtils.parseSdp(sdp);
        assertThat(result.getSsrc()).isEqualTo("0100000001");
        assertThat(result.getMediaDescription()).isEqualTo("v/2/4");
    }

    @Test
    void parseSdp_withoutYandF_returnsNullSsrc() {
        String sdp = "v=0\r\n" +
                "o=- 0 0 IN IP4 192.168.1.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 192.168.1.1\r\n" +
                "t=0 0\r\n" +
                "m=video 9000 RTP/AVP 96\r\n";
        GbSessionDescription result = (GbSessionDescription) SipUtils.parseSdp(sdp);
        assertThat(result.getSsrc()).isNull();
    }

    @Test
    void getSubjectId_parsesFirstSegment() {
        Request request = mock(Request.class);
        SubjectHeader subjectHeader = mock(SubjectHeader.class);
        when(request.getHeader(SubjectHeader.NAME)).thenReturn(subjectHeader);
        when(subjectHeader.getSubject()).thenReturn("34020000001320000001:0");
        assertThat(SipUtils.getSubjectId(request)).isEqualTo("34020000001320000001");
    }

    @Test
    void getSubjectId_missingHeader_returnsNull() {
        Request request = mock(Request.class);
        when(request.getHeader(SubjectHeader.NAME)).thenReturn(null);
        assertThat(SipUtils.getSubjectId(request)).isNull();
    }
}
