package io.github.lunasaw.sip.common.transmit.request;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.SipMessage;
import io.github.lunasaw.sip.common.entity.ToDevice;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SIP请求构建器工厂测试类
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class SipRequestBuilderFactoryTest {

    @Test
    void testCreateMessageRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String content = "<test>message content</test>";
        String callId = "test-call-id-123";

        // 执行测试
        SIPRequest request = (SIPRequest) SipRequestBuilderFactory.createMessageRequest(fromDevice, toDevice, content, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.MESSAGE, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
        assertEquals(content, request.getContent());
    }

    @Test
    void testCreateInviteRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String content = "v=0\r\no=- 0 0 IN IP4 192.168.1.100\r\ns=Play\r\nc=IN IP4 192.168.1.100\r\nt=0 0\r\nm=video 6000 RTP/AVP 96\r\na=recvonly\r\na=rtpmap:96 PS/90000\r\n";
        String subject = "34020000001320000001:0,34020000001320000002:0";
        String callId = "test-call-id-456";

        // 执行测试
        Request request = SipRequestBuilderFactory.createInviteRequest(fromDevice, toDevice, content, subject, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.INVITE, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
        assertEquals(content, request.getContent());
        assertNotNull(request.getHeader("Subject"));
    }

    @Test
    void testCreateByeRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String callId = "test-call-id-789";

        // 执行测试
        Request request = SipRequestBuilderFactory.createByeRequest(fromDevice, toDevice, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.BYE, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
    }

    @Test
    void testCreateRegisterRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        Integer expires = 3600;
        String callId = "test-call-id-101";

        // 执行测试
        Request request = SipRequestBuilderFactory.createRegisterRequest(fromDevice, toDevice, expires, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.REGISTER, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
        assertNotNull(request.getHeader("Expires"));
    }

    @Test
    void testCreateAckRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String callId = "test-call-id-202";

        // 执行测试
        Request request = SipRequestBuilderFactory.createAckRequest(fromDevice, toDevice, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.ACK, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
    }

    @Test
    void testCreateAckRequestWithContent() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String content = "v=0\r\no=- 0 0 IN IP4 192.168.1.100\r\ns=Play\r\nc=IN IP4 192.168.1.100\r\nt=0 0\r\nm=video 6000 RTP/AVP 96\r\na=recvonly\r\na=rtpmap:96 PS/90000\r\n";
        String callId = "test-call-id-303";

        // 执行测试
        Request request = SipRequestBuilderFactory.createAckRequest(fromDevice, toDevice, content, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.ACK, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
        assertEquals(content, request.getContent());
    }

    @Test
    void testCreateInfoRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        String content = "<test>info content</test>";
        String callId = "test-call-id-404";

        // 执行测试
        Request request = SipRequestBuilderFactory.createInfoRequest(fromDevice, toDevice, content, callId);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.INFO, request.getMethod());
        assertEquals(callId, ((SIPRequest) request).getCallIdHeader().getCallId());
        assertEquals(content, request.getContent());
    }

    @Test
    void testCreateSipRequest() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        SipMessage sipMessage = SipMessage.getMessageBody();
        sipMessage.setMethod(Request.MESSAGE);
        sipMessage.setContent("test content");
        sipMessage.setCallId("test-call-id-505");

        // 执行测试
        Request request = SipRequestBuilderFactory.createSipRequest(fromDevice, toDevice, sipMessage);

        // 验证结果
        assertNotNull(request);
        assertEquals(Request.MESSAGE, request.getMethod());
        assertEquals("test-call-id-505", ((SIPRequest) request).getCallIdHeader().getCallId());
        assertEquals("test content", request.getContent());
    }

    @Test
    void testCreateSipRequestWithUnsupportedMethod() {
        // 准备测试数据
        FromDevice fromDevice = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
        ToDevice toDevice = ToDevice.getInstance("34020000001320000002", "192.168.1.200", 5060);
        SipMessage sipMessage = SipMessage.getMessageBody();
        sipMessage.setMethod("UNSUPPORTED_METHOD");
        sipMessage.setCallId("test-call-id-606");

        // 执行测试并验证异常
        assertThrows(IllegalArgumentException.class, () -> {
            SipRequestBuilderFactory.createSipRequest(fromDevice, toDevice, sipMessage);
        });
    }

    @Test
    void testGetBuilders() {
        // 验证所有构建器都能正常获取
        assertNotNull(SipRequestBuilderFactory.getMessageBuilder());
        assertNotNull(SipRequestBuilderFactory.getInviteBuilder());
        assertNotNull(SipRequestBuilderFactory.getByeBuilder());
        assertNotNull(SipRequestBuilderFactory.getRegisterBuilder());
        assertNotNull(SipRequestBuilderFactory.getSubscribeBuilder());
        assertNotNull(SipRequestBuilderFactory.getInfoBuilder());
        assertNotNull(SipRequestBuilderFactory.getAckBuilder());
        assertNotNull(SipRequestBuilderFactory.getNotifyBuilder());
    }
}