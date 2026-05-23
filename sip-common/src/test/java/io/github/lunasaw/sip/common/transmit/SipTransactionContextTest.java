package io.github.lunasaw.sip.common.transmit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.Header;
import javax.sip.message.Request;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SipTransactionContextTest {

    @Mock
    private RequestEvent requestEvent;
    @Mock
    private ServerTransaction serverTransaction;
    @Mock
    private Request request;
    @Mock
    private Header callIdHeader;
    @Mock
    private Header fromHeader;
    @Mock
    private Header toHeader;
    @Mock
    private Header viaHeader;
    @Mock
    private Header cseqHeader;

    @BeforeEach
    void setUp() {
        SipTransactionContext.TRANSACTION_CONTEXTS.clear();
        SipTransactionContext.clearCurrentContext();

        when(requestEvent.getRequest()).thenReturn(request);
        when(request.getHeader("Call-ID")).thenReturn(callIdHeader);
        when(request.getHeader("From")).thenReturn(fromHeader);
        when(request.getHeader("To")).thenReturn(toHeader);
        when(request.getHeader("Via")).thenReturn(viaHeader);
        when(request.getHeader("CSeq")).thenReturn(cseqHeader);
        when(callIdHeader.toString()).thenReturn("Call-ID: abc123@host");
        when(fromHeader.toString()).thenReturn("From: <sip:user@host>;tag=fromtag1");
        when(toHeader.toString()).thenReturn("To: <sip:server@host>");
        when(viaHeader.toString()).thenReturn("Via: SIP/2.0/UDP host;branch=z9hG4bK123");
        when(cseqHeader.toString()).thenReturn("CSeq: 1 REGISTER");
    }

    @AfterEach
    void tearDown() {
        SipTransactionContext.TRANSACTION_CONTEXTS.clear();
        SipTransactionContext.clearCurrentContext();
    }

    @Test
    void createContext_storesAndSetsCurrentContext() {
        SipTransactionContext.TransactionContextInfo ctx =
                SipTransactionContext.createContext(requestEvent, serverTransaction);

        assertThat(ctx).isNotNull();
        assertThat(SipTransactionContext.TRANSACTION_CONTEXTS).containsKey(ctx.getContextKey());
        assertThat(SipTransactionContext.getCurrentContext()).isSameAs(ctx);
    }

    @Test
    void getContext_unknownKey_returnsNull() {
        assertThat(SipTransactionContext.getContext("nonexistent")).isNull();
    }

    @Test
    void getContext_existingKey_returnsContext() {
        SipTransactionContext.TransactionContextInfo ctx =
                SipTransactionContext.createContext(requestEvent, serverTransaction);
        assertThat(SipTransactionContext.getContext(ctx.getContextKey())).isSameAs(ctx);
    }

    @Test
    void removeContext_removesFromMapAndInvalidates() {
        SipTransactionContext.TransactionContextInfo ctx =
                SipTransactionContext.createContext(requestEvent, serverTransaction);
        String key = ctx.getContextKey();

        SipTransactionContext.removeContext(key);

        assertThat(SipTransactionContext.TRANSACTION_CONTEXTS).doesNotContainKey(key);
        assertThat(ctx.isValid()).isFalse();
    }

    @Test
    void clearCurrentContext_makesGetCurrentContextReturnNull() {
        SipTransactionContext.createContext(requestEvent, serverTransaction);
        SipTransactionContext.clearCurrentContext();
        assertThat(SipTransactionContext.getCurrentContext()).isNull();
    }

    @Test
    void cleanupExpiredContexts_removesInvalidContexts() {
        SipTransactionContext.TransactionContextInfo ctx =
                SipTransactionContext.createContext(requestEvent, serverTransaction);
        String key = ctx.getContextKey();

        ctx.invalidate();
        SipTransactionContext.cleanupExpiredContexts();

        assertThat(SipTransactionContext.TRANSACTION_CONTEXTS).doesNotContainKey(key);
    }

    @Test
    void getContextStats_returnsFormattedString() {
        SipTransactionContext.createContext(requestEvent, serverTransaction);
        String stats = SipTransactionContext.getContextStats();
        assertThat(stats).contains("总上下文数").contains("有效上下文数");
    }
}
