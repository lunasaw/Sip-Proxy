package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.request.invite.ServerInviteRequestHandler;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 测试专用的InviteRequestHandler实现
 * 用于验证INVITE请求的处理流程和测试钩子
 */
@Component
@Primary
@Slf4j
public class TestServerInviteRequestHandler implements ServerInviteRequestHandler {

    // === 测试辅助字段 ===
    private static CountDownLatch invitePlayLatch;
    private static AtomicBoolean invitePlayReceived = new AtomicBoolean(false);
    private static AtomicReference<String> receivedInvitePlayCallId = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlaySdp = new AtomicReference<>();

    private static CountDownLatch invitePlayBackLatch;
    private static AtomicBoolean invitePlayBackReceived = new AtomicBoolean(false);
    private static AtomicReference<String> receivedInvitePlayBackCallId = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlayBackSdp = new AtomicReference<>();

    @Override
    public void inviteSession(String callId, SdpSessionDescription sessionDescription) {
        log.info("[ClientTest] 处理INVITE会话: callId={}, sessionDescription={}", callId, sessionDescription);

        // 从sessionDescription中获取原始SDP内容
        String sdpContent = null;
        try {
            if (sessionDescription != null && sessionDescription.getBaseSdb() != null) {
                sdpContent = sessionDescription.getBaseSdb().toString();
            }
        } catch (Exception e) {
            log.warn("获取SDP原始内容失败: callId={}", callId, e);
        }
        
        // 如果获取不到SDP内容，使用toString()的结果
        if (sdpContent == null) {
            sdpContent = sessionDescription != null ? sessionDescription.toString() : "";
        }
        
        updateTestHook(callId, sdpContent, sessionDescription);
    }

    @Override
    public String getInviteResponse(String userId, SdpSessionDescription sessionDescription) {
        log.info("[ClientTest] 获取INVITE响应: userId={}, sessionDescription={}", userId, sessionDescription);

        // 返回一个简单的SDP响应内容用于测试
        return "v=0\r\n" +
               "o=" + userId + " 0 0 IN IP4 127.0.0.1\r\n" +
               "s=Play\r\n" +
               "c=IN IP4 127.0.0.1\r\n" +
               "t=0 0\r\n" +
               "m=video 6000 RTP/AVP 96\r\n" +
               "a=rtpmap:96 PS/90000\r\n";
    }

    /**
     * 更新测试钩子状态
     */
    private void updateTestHook(String callId, String sdpContent, SdpSessionDescription sessionDescription) {
        try {
            log.info("📋 分析SDP内容: callId={}, sdpContent={}, sessionDescription={}", callId, sdpContent, sessionDescription);
            
            // 根据SDP内容判断是实时点播还是回放点播
            if (sdpContent.contains("s=PlayBack")) {
                // 回放点播
                updateInvitePlayBack(callId, sdpContent);
                log.info("📼 更新回放点播测试钩子: callId={}", callId);
            } else if (sdpContent.contains("s=Play")) {
                // 实时点播
                updateInvitePlay(callId, sdpContent);
                log.info("📺 更新实时点播测试钩子: callId={}", callId);
            } else {
                // 其他类型的Invite请求
                log.info("📺 收到其他类型INVITE请求: callId={}, sdp={}", callId, sessionDescription);
            }
        } catch (Exception e) {
            log.warn("更新测试钩子时发生异常: callId={}", callId, e);
        }
    }

    // === 实时点播测试钩子 ===
    public void updateInvitePlay(String callId, String sdpContent) {
        log.info("[ClientTest] 更新实时点播: callId={}", callId);
        invitePlayReceived.set(true);
        receivedInvitePlayCallId.set(callId);
        receivedInvitePlaySdp.set(sdpContent);
        if (invitePlayLatch != null) invitePlayLatch.countDown();
    }

    public static void resetInvitePlayTestState() {
        invitePlayLatch = new CountDownLatch(1);
        invitePlayReceived.set(false);
        receivedInvitePlayCallId.set(null);
        receivedInvitePlaySdp.set(null);
    }

    public static boolean waitForInvitePlay(long timeout, TimeUnit unit) throws InterruptedException {
        if (invitePlayLatch == null) return false;
        return invitePlayLatch.await(timeout, unit);
    }

    public static boolean hasReceivedInvitePlay() {
        return invitePlayReceived.get();
    }

    public static String getReceivedInvitePlayCallId() {
        return receivedInvitePlayCallId.get();
    }

    public static String getReceivedInvitePlaySdp() {
        return receivedInvitePlaySdp.get();
    }

    // === 回放点播测试钩子 ===
    public void updateInvitePlayBack(String callId, String sdpContent) {
        log.info("[ClientTest] 更新回放点播: callId={}", callId);
        invitePlayBackReceived.set(true);
        receivedInvitePlayBackCallId.set(callId);
        receivedInvitePlayBackSdp.set(sdpContent);
        if (invitePlayBackLatch != null) invitePlayBackLatch.countDown();
    }

    public static void resetInvitePlayBackTestState() {
        invitePlayBackLatch = new CountDownLatch(1);
        invitePlayBackReceived.set(false);
        receivedInvitePlayBackCallId.set(null);
        receivedInvitePlayBackSdp.set(null);
    }

    public static boolean waitForInvitePlayBack(long timeout, TimeUnit unit) throws InterruptedException {
        if (invitePlayBackLatch == null) return false;
        return invitePlayBackLatch.await(timeout, unit);
    }

    public static boolean hasReceivedInvitePlayBack() {
        return invitePlayBackReceived.get();
    }

    public static String getReceivedInvitePlayBackCallId() {
        return receivedInvitePlayBackCallId.get();
    }

    public static String getReceivedInvitePlayBackSdp() {
        return receivedInvitePlayBackSdp.get();
    }

    // === 重置所有测试状态 ===
    public static void resetTestState() {
        resetInvitePlayTestState();
        resetInvitePlayBackTestState();
    }
}