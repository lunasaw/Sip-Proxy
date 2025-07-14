package io.github.lunasaw.sip.common.transmit;

import com.luna.common.os.SystemInfoUtil;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.test.ApplicationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * ResponseCmd 重构后的单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class ResponseCmdTest extends ApplicationTest {

    @Mock
    private RequestEvent requestEvent;

    @Mock
    private SIPRequest sipRequest;

    @Mock
    private ServerTransaction serverTransaction;

    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;

    // 使用真实的SIP Header对象而不是mock对象
    private ContactHeader contactHeader;
    private ContentTypeHeader contentTypeHeader;

    @BeforeEach
    @SneakyThrows
    public void setUp() {
        super.setUp();
        // 初始化SIP协议栈
        initializeSipStack();

        // 创建真实的SIP请求对象
        sipRequest = createRealSipRequest();

        // 创建真实的SIP Header对象
        createRealSipHeaders();

        // 设置mock行为
        when(requestEvent.getRequest()).thenReturn(sipRequest);
        when(requestEvent.getServerTransaction()).thenReturn(serverTransaction);

    }

    /**
     * 初始化SIP协议栈
     */
    private void initializeSipStack() throws Exception {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        messageFactory = sipFactory.createMessageFactory();
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();

        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "test-stack");
        properties.setProperty("javax.sip.IP_ADDRESS", "127.0.0.1");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "sipdebug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "siplog.txt");
        properties.setProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE", "1048576");
        properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "1");
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");

    }

    /**
     * 创建真实的SIP Header对象
     */
    private void createRealSipHeaders() throws Exception {
        // 创建真实的ContactHeader
        SipURI contactUri = addressFactory.createSipURI("test-user", "127.0.0.1:5060");
        Address contactAddress = addressFactory.createAddress(contactUri);
        contactHeader = headerFactory.createContactHeader(contactAddress);

        // 创建真实的ContentTypeHeader
        contentTypeHeader = headerFactory.createContentTypeHeader("application", "xml");
    }

    /**
     * 创建真实的SIP请求对象
     */
    private SIPRequest createRealSipRequest() throws Exception {
        // 创建CallId
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader("test-call-id");

        // 创建CSeq
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, "MESSAGE");

        // 创建From
        SipURI fromUri = addressFactory.createSipURI("test-user", "127.0.0.1:5060");
        Address fromAddress = addressFactory.createAddress(fromUri);
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, "from-tag");

        // 创建To
        SipURI toUri = addressFactory.createSipURI("test-user", "127.0.0.1:5060");
        Address toAddress = addressFactory.createAddress(toUri);
        ToHeader toHeader = headerFactory.createToHeader(toAddress, "to-tag");

        // 创建Via
        ViaHeader viaHeader = headerFactory.createViaHeader(SystemInfoUtil.getIP(), 5060, "UDP", "branch-test");
        List<ViaHeader> viaHeaders = new ArrayList<>();
        viaHeaders.add(viaHeader);

        // 创建MaxForwards
        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

        // 创建RequestURI
        SipURI requestUri = addressFactory.createSipURI("test-user", "127.0.0.1:5060");

        // 创建真实的SIP请求
        SIPRequest request = (SIPRequest) messageFactory.createRequest(
                requestUri,
                "MESSAGE",
                callIdHeader,
                cSeqHeader,
                fromHeader,
                toHeader,
                viaHeaders,
                maxForwardsHeader
        );

        return request;
    }

    @Test
    void testResponseBuilderBasicUsage() {
        // 设置mock行为
        when(requestEvent.getRequest()).thenReturn(sipRequest);
        when(requestEvent.getServerTransaction()).thenReturn(serverTransaction);

        // 测试基本用法
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithPhrase() {
        // 测试带短语的响应
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .phrase("OK")
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithContent() {
        // 测试带内容的响应
        String content = "<?xml version=\"1.0\"?><Response>OK</Response>";
        ContentTypeHeader contentType = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();

        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .content(content)
                    .contentType(contentType)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithHeaders() {
        // 测试带响应头的响应
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .header(contactHeader)
                    .send();
        });
    }

    @Test
    void testResponseBuilderMultipleHeaders() {
        // 测试多个响应头
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .headers(contactHeader, contentTypeHeader)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithCustomIp() {
        // 测试自定义IP地址
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .ip("10.0.0.1")
                    .send();
        });
    }

    @Test
    void testConvenienceMethods() {
        // 测试便捷方法

        assertDoesNotThrow(() -> {
            ResponseCmd.sendResponse(200, requestEvent);
            ResponseCmd.sendResponse(200, "OK", requestEvent);
            ResponseCmd.sendResponseNoTransaction(200, requestEvent);
            ResponseCmd.sendResponseNoTransaction(200, "OK", requestEvent);
        });
    }

    @Test
    void testConvenienceMethodsWithContent() {
        // 测试带内容的便捷方法
        String content = "test content";
        ContentTypeHeader contentType = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();

        assertDoesNotThrow(() -> {
            ResponseCmd.sendResponse(200, content, contentType, requestEvent);
            ResponseCmd.sendResponseNoTransaction(200, content, contentType, requestEvent);
        });
    }

    @Test
    void testDeprecatedMethodsStillWork() {
        // 测试废弃方法仍然可用
        assertDoesNotThrow(() -> {
            ResponseCmd.doResponseCmd(200, requestEvent);
            ResponseCmd.doResponseCmd(200, "OK", requestEvent);
            ResponseCmd.doResponseCmdNoTransaction(200, requestEvent);
        });
    }

    @Test
    void testComplexResponseBuilder() {
        // 测试复杂的响应构建
        String content = "<?xml version=\"1.0\"?><Response>OK</Response>";
        ContentTypeHeader contentType = ContentTypeEnum.APPLICATION_XML.getContentTypeHeader();

        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .phrase("OK")
                    .content(content)
                    .contentType(contentType)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .header(contactHeader)
                    .ip(SystemInfoUtil.getIP())
                    .send();
        });
    }


    @Test
    void testResponseBuilderWithNullTransaction() {
        // 测试空事务的情况 - 应该自动降级到无事务模式
        when(requestEvent.getServerTransaction()).thenReturn(null);

        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderChaining() {
        // 测试方法链式调用
        ResponseCmd.SipResponseBuilder builder = ResponseCmd.response(200)
                .phrase("OK")
                .content("test")
                .contentType(contentTypeHeader)
                .requestEvent(requestEvent)
                .request(sipRequest)
                .header(contactHeader)
                .ip(SystemInfoUtil.getIP());

        assertNotNull(builder);
        assertDoesNotThrow(builder::send);
    }

    @Test
    void testResponseBuilderWithDifferentStatusCodes() {
        // 测试不同的状态码
        int[] statusCodes = {100, 200, 400, 401, 404, 500, 503};

        for (int statusCode : statusCodes) {
            final int code = statusCode;
            assertDoesNotThrow(() -> {
                ResponseCmd.response(code)
                        .requestEvent(requestEvent)
                        .request(sipRequest)
                        .send();
            });
        }
    }

    @Test
    void testResponseBuilderWithEmptyContent() {
        // 测试空内容
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .content("")
                    .contentType(contentTypeHeader)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithNullContent() {
        // 测试null内容
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .content(null)
                    .contentType(contentTypeHeader)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithEmptyPhrase() {
        // 测试空短语
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .phrase("")
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }

    @Test
    void testResponseBuilderWithNullPhrase() {
        // 测试null短语
        assertDoesNotThrow(() -> {
            ResponseCmd.response(200)
                    .phrase(null)
                    .requestEvent(requestEvent)
                    .request(sipRequest)
                    .send();
        });
    }
}