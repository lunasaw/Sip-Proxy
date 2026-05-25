package io.github.lunasaw.sip.common.utils;

import org.junit.jupiter.api.Test;

import javax.sip.header.SubjectHeader;
import javax.sip.message.Request;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SipUtilsTest {

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
