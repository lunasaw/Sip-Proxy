package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.request.register.RegisterInfo;
import io.github.lunasaw.gbproxy.server.transmit.request.register.ServerRegisterProcessorHandler;
import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 测试专用的ServerRegisterProcessorHandler实现
 * 用于验证REGISTER请求的处理流程
 *
 * @author claude
 * @date 2025/07/21
 */
@Component
@Primary
@Slf4j
public class TestServerRegisterProcessorHandler implements ServerRegisterProcessorHandler {

    // 静态字段用于测试验证
    private static CountDownLatch registerLatch;
    private static AtomicBoolean registerReceived = new AtomicBoolean(false);
    private static AtomicReference<String> registeredUserId = new AtomicReference<>();
    private static AtomicReference<RegisterInfo> receivedRegisterInfo = new AtomicReference<>();

    private static CountDownLatch unauthorizedLatch;
    private static AtomicBoolean unauthorizedReceived = new AtomicBoolean(false);
    private static AtomicReference<String> unauthorizedUserId = new AtomicReference<>();

    private static CountDownLatch deviceOnlineLatch;
    private static AtomicBoolean deviceOnlineReceived = new AtomicBoolean(false);
    private static AtomicReference<String> onlineUserId = new AtomicReference<>();

    private static CountDownLatch deviceOfflineLatch;
    private static AtomicBoolean deviceOfflineReceived = new AtomicBoolean(false);
    private static AtomicReference<String> offlineUserId = new AtomicReference<>();

    @Override
    public void handleUnauthorized(String userId, RequestEvent evt) {
        log.info("🔐 TestServerRegisterProcessorHandler 处理401未授权: userId={}", userId);
        unauthorizedReceived.set(true);
        unauthorizedUserId.set(userId);
        if (unauthorizedLatch != null) {
            unauthorizedLatch.countDown();
        }
    }

    @Override
    public SipTransaction getDeviceTransaction(String userId) {
        log.info("🔍 TestServerRegisterProcessorHandler 获取设备事务: userId={}", userId);
        return null; // 测试中返回null表示没有现有事务
    }

    @Override
    public void handleRegisterInfoUpdate(String userId, RegisterInfo registerInfo, RequestEvent evt) {
        log.info("📝 TestServerRegisterProcessorHandler 更新注册信息: userId={}, registerInfo={}", userId, registerInfo);
        registerReceived.set(true);
        registeredUserId.set(userId);
        receivedRegisterInfo.set(registerInfo);
        if (registerLatch != null) {
            registerLatch.countDown();
        }
    }

    @Override
    public void handleDeviceOnline(String userId, SipTransaction sipTransaction, RequestEvent evt) {
        log.info("🟢 TestServerRegisterProcessorHandler 设备上线: userId={}", userId);
        deviceOnlineReceived.set(true);
        onlineUserId.set(userId);
        if (deviceOnlineLatch != null) {
            deviceOnlineLatch.countDown();
        }
    }

    @Override
    public void handleDeviceOffline(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction, RequestEvent evt) {
        log.info("🔴 TestServerRegisterProcessorHandler 设备下线: userId={}", userId);
        deviceOfflineReceived.set(true);
        offlineUserId.set(userId);
        if (deviceOfflineLatch != null) {
            deviceOfflineLatch.countDown();
        }
    }

    @Override
    public Integer getDeviceExpire(String userId) {
        log.info("⏰ TestServerRegisterProcessorHandler 获取设备过期时间: userId={}", userId);
        return 3600; // 1小时
    }

    @Override
    public boolean validatePassword(String userId, String password, RequestEvent evt) {
        log.info("🔓 TestServerRegisterProcessorHandler 验证密码: userId={}, password={}", userId, password);
        return true; // 测试中总是返回验证成功
    }

    // ==================== 测试工具方法 ====================

    /**
     * 重置测试状态
     */
    public static void resetTestState() {
        registerLatch = new CountDownLatch(1);
        registerReceived.set(false);
        registeredUserId.set(null);
        receivedRegisterInfo.set(null);

        unauthorizedLatch = new CountDownLatch(1);
        unauthorizedReceived.set(false);
        unauthorizedUserId.set(null);

        deviceOnlineLatch = new CountDownLatch(1);
        deviceOnlineReceived.set(false);
        onlineUserId.set(null);

        deviceOfflineLatch = new CountDownLatch(1);
        deviceOfflineReceived.set(false);
        offlineUserId.set(null);

        log.info("🔄 TestServerRegisterProcessorHandler 测试状态已重置");
    }

    // === 注册相关 ===
    public static boolean waitForRegister(long timeout, TimeUnit unit) throws InterruptedException {
        if (registerLatch == null) return false;
        return registerLatch.await(timeout, unit);
    }

    public static boolean hasReceivedRegister() {
        return registerReceived.get();
    }

    public static String getRegisteredUserId() {
        return registeredUserId.get();
    }

    public static RegisterInfo getReceivedRegisterInfo() {
        return receivedRegisterInfo.get();
    }

    // === 401未授权相关 ===
    public static boolean waitForUnauthorized(long timeout, TimeUnit unit) throws InterruptedException {
        if (unauthorizedLatch == null) return false;
        return unauthorizedLatch.await(timeout, unit);
    }

    public static boolean hasReceivedUnauthorized() {
        return unauthorizedReceived.get();
    }

    public static String getUnauthorizedUserId() {
        return unauthorizedUserId.get();
    }

    // === 设备上线相关 ===
    public static boolean waitForDeviceOnline(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceOnlineLatch == null) return false;
        return deviceOnlineLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceOnline() {
        return deviceOnlineReceived.get();
    }

    public static String getOnlineUserId() {
        return onlineUserId.get();
    }

    // === 设备下线相关 ===
    public static boolean waitForDeviceOffline(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceOfflineLatch == null) return false;
        return deviceOfflineLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceOffline() {
        return deviceOfflineReceived.get();
    }

    public static String getOfflineUserId() {
        return offlineUserId.get();
    }
}