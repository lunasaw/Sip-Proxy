package io.github.lunasaw.gbproxy.test;

import io.github.lunasaw.gbproxy.test.handler.TestServerInviteRequestHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.springframework.test.context.TestPropertySource;

/**
 * INVITE请求处理器测试
 * 演示如何使用新的测试架构进行端到端测试
 *
 * @author luna
 */
@SpringBootTest(classes = Gb28181ApplicationTest.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "logging.level.io.github.lunasaw.sip=DEBUG",
    "logging.level.io.github.lunasaw.gbproxy=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class InviteRequestProcessorTest extends BasicSipCommonTest {

    @Test
    @Order(1)
    void testInvitePlayRequest() throws InterruptedException {
        TestServerInviteRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        log.info("🧪 开始测试INVITE实时点播请求");

        // 构造实时点播的SDP内容
        String sdpContent = "v=0\r\n" +
                "o=41010500002000000001 0 0 IN IP4 127.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 6000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n";

        // 发送INVITE请求
        String callId = SipSender.doInviteRequest(serverFromDevice, serverToDevice, sdpContent, "test-subject");

        log.info("📤 发送INVITE实时点播请求: callId={}", callId);

        // 等待测试钩子触发
        boolean received = TestServerInviteRequestHandler.waitForInvitePlay(5, TimeUnit.SECONDS);
        assertTrue(received, "应该收到INVITE实时点播请求");

        // 验证测试钩子状态
        assertTrue(TestServerInviteRequestHandler.hasReceivedInvitePlay(), "应该标记为已收到实时点播");
        assertEquals(callId, TestServerInviteRequestHandler.getReceivedInvitePlayCallId(), "CallId应该匹配");
        assertNotNull(TestServerInviteRequestHandler.getReceivedInvitePlaySdp(), "SDP内容不应该为空");

        log.info("✅ INVITE实时点播请求测试通过");
    }

    @Test
    @Order(2)
    void testInvitePlayBackRequest() throws InterruptedException {
        TestServerInviteRequestHandler.resetTestState();
        FromDevice serverFromDevice = deviceSupplier.getServerFromDevice();
        ToDevice serverToDevice = deviceSupplier.getServerToDevice();
        log.info("🧪 开始测试INVITE回放点播请求");

        // 构造回放点播的SDP内容
        String sdpContent = "v=0\r\n" +
                "o=41010500002000000001 0 0 IN IP4 127.0.0.1\r\n" +
                "s=PlayBack\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=1704067200 1704153600\r\n" +
                "m=video 6000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n";

        // 发送INVITE请求
        String callId = SipSender.doInviteRequest(serverFromDevice, serverToDevice, sdpContent, "test-subject");

        log.info("📤 发送INVITE回放点播请求: callId={}", callId);

        // 等待测试钩子触发
        boolean received = TestServerInviteRequestHandler.waitForInvitePlayBack(5, TimeUnit.SECONDS);
        assertTrue(received, "应该收到INVITE回放点播请求");

        // 验证测试钩子状态
        assertTrue(TestServerInviteRequestHandler.hasReceivedInvitePlayBack(), "应该标记为已收到回放点播");
        assertEquals(callId, TestServerInviteRequestHandler.getReceivedInvitePlayBackCallId(), "CallId应该匹配");
        assertNotNull(TestServerInviteRequestHandler.getReceivedInvitePlayBackSdp(), "SDP内容不应该为空");

        log.info("✅ INVITE回放点播请求测试通过");
    }

    @Test
    @Order(3)
    void testInviteRequestHandlerBusinessLogic() throws Exception {
        TestServerInviteRequestHandler.resetTestState();
        log.info("🧪 开始测试InviteRequestHandler业务逻辑");

        // 创建测试用的SDP内容 - 按照GB28181协议标准
        String testSdpContent = "v=0\r\n" +
                "o=41010500002000000001 0 0 IN IP4 127.0.0.1\r\n" +
                "s=Play\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=0 0\r\n" +
                "m=video 6000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "y=0123456789ABCDEF\r\n" +    // GB28181特有的SSRC字段
                "f=v/////a/1/8/1\r\n";        // GB28181特有的媒体格式字段

        // 使用SipUtils.parseSdp创建SdpSessionDescription对象
        io.github.lunasaw.sip.common.entity.SdpSessionDescription sessionDescription = 
            io.github.lunasaw.sip.common.utils.SipUtils.parseSdp(testSdpContent);
        
        log.info("📋 创建的SDP会话描述: {}", sessionDescription);

        TestServerInviteRequestHandler handler = new TestServerInviteRequestHandler();

        // 测试inviteSession方法 - 验证会话处理逻辑
        String callId = "test-call-id-123";
        handler.inviteSession(callId, sessionDescription);
        
        // 验证测试钩子状态 - 应该识别为实时点播
        assertTrue(TestServerInviteRequestHandler.hasReceivedInvitePlay(), "应该识别为实时点播请求");
        assertEquals(callId, TestServerInviteRequestHandler.getReceivedInvitePlayCallId(), "CallId应该匹配");
        assertNotNull(TestServerInviteRequestHandler.getReceivedInvitePlaySdp(), "接收的SDP内容不应该为空");

        // 测试getInviteResponse方法 - 验证响应生成逻辑
        String userId = "41010500002000000001";
        String response = handler.getInviteResponse(userId, sessionDescription);

        // 验证响应内容包含必要的SDP字段
        assertNotNull(response, "响应内容不应该为空");
        assertTrue(response.contains("v=0"), "响应应该包含SDP版本");
        assertTrue(response.contains("s=Play"), "响应应该包含会话名称");
        assertTrue(response.contains("m=video"), "响应应该包含视频媒体描述");
        assertTrue(response.contains("o=" + userId), "响应应该包含正确的用户ID");
        assertTrue(response.contains("a=rtpmap:96 PS/90000"), "响应应该包含RTP映射");
        
        log.info("📤 生成的响应SDP: {}", response);

        // 测试回放点播场景
        TestServerInviteRequestHandler.resetTestState();
        String playbackSdpContent = "v=0\r\n" +
                "o=41010500002000000001 0 0 IN IP4 127.0.0.1\r\n" +
                "s=PlayBack\r\n" +
                "c=IN IP4 127.0.0.1\r\n" +
                "t=1704067200 1704153600\r\n" +  // 指定回放时间范围
                "m=video 6000 RTP/AVP 96\r\n" +
                "a=rtpmap:96 PS/90000\r\n" +
                "y=FEDCBA9876543210\r\n" +    // 不同的SSRC
                "f=v/////a/1/8/1\r\n";

        io.github.lunasaw.sip.common.entity.SdpSessionDescription playbackSessionDescription = 
            io.github.lunasaw.sip.common.utils.SipUtils.parseSdp(playbackSdpContent);

        String playbackCallId = "test-playback-call-id-456";
        handler.inviteSession(playbackCallId, playbackSessionDescription);

        // 验证回放点播钩子状态
        assertTrue(TestServerInviteRequestHandler.hasReceivedInvitePlayBack(), "应该识别为回放点播请求");
        assertEquals(playbackCallId, TestServerInviteRequestHandler.getReceivedInvitePlayBackCallId(), "回放CallId应该匹配");
        assertNotNull(TestServerInviteRequestHandler.getReceivedInvitePlayBackSdp(), "回放SDP内容不应该为空");

        // 验证GB28181特有字段解析
        if (sessionDescription instanceof io.github.lunasaw.sip.common.entity.GbSessionDescription) {
            io.github.lunasaw.sip.common.entity.GbSessionDescription gbSdp = 
                (io.github.lunasaw.sip.common.entity.GbSessionDescription) sessionDescription;
            assertNotNull(gbSdp.getSsrc(), "SSRC字段应该被正确解析");
            assertNotNull(gbSdp.getMediaDescription(), "媒体描述字段应该被正确解析");
            log.info("🎯 GB28181字段解析成功: SSRC={}, MediaDescription={}", gbSdp.getSsrc(), gbSdp.getMediaDescription());
        }

        log.info("✅ InviteRequestHandler业务逻辑测试通过");
    }
}