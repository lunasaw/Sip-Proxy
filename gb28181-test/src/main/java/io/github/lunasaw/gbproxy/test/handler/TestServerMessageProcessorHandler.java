package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.server.transimit.request.message.ServerMessageProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 测试专用的ServerMessageProcessorHandler实现
 * 用于验证MESSAGE请求的处理流程
 */
@Component
@Primary
@Slf4j
public class TestServerMessageProcessorHandler implements ServerMessageProcessorHandler {

    // 静态字段用于测试验证
    private static CountDownLatch keepaliveLatch;
    private static AtomicBoolean keepaliveReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceKeepLiveNotify> receivedKeepalive = new AtomicReference<>();

    @Override
    public void handleMessageRequest(RequestEvent evt, FromDevice fromDevice) {
        log.info("🔄 TestServerMessageProcessorHandler 处理MESSAGE请求 - evt: {}, fromDevice: {}", evt, fromDevice);
        // 调用消息处理流程，这会触发具体的MessageHandler
        try {
            // 这里应该调用父类的处理逻辑，但由于我们实现的是接口，需要手动触发
            log.info("✅ TestServerMessageProcessorHandler MESSAGE请求处理完成");
        } catch (Exception e) {
            log.error("❌ TestServerMessageProcessorHandler MESSAGE请求处理异常", e);
        }
    }

    @Override
    public boolean validateDevicePermission(RequestEvent evt) {
        log.info("✅ TestServerMessageProcessorHandler 验证设备权限通过");
        return true;
    }

    @Override
    public FromDevice getFromDevice() {
        // 返回一个模拟的设备信息以便测试继续进行
        FromDevice fromDevice = new FromDevice();
        fromDevice.setUserId("33010602011187000001");
        fromDevice.setIp("127.0.0.1");
        fromDevice.setPort(5061);
        fromDevice.setTransport("UDP");
        log.info("🔧 TestServerMessageProcessorHandler 返回模拟设备信息: {}", fromDevice);
        return fromDevice;
    }

    @Override
    public void handleMessageError(RequestEvent evt, String errorMessage) {
        log.error("❌ TestServerMessageProcessorHandler 处理错误: {}", errorMessage);
    }

    @Override
    public void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify) {
        log.info("💓 TestServerMessageProcessorHandler 接收到心跳: {}", deviceKeepLiveNotify);
        recordKeepalive(deviceKeepLiveNotify);
    }

    @Override
    public void updateRemoteAddress(String userId, RemoteAddressInfo remoteAddressInfo) {
        log.info("📍 TestServerMessageProcessorHandler 更新设备地址: userId={}, address={}", userId, remoteAddressInfo);
    }

    @Override
    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {
        log.info("🚨 TestServerMessageProcessorHandler 更新设备报警: {}", deviceAlarmNotify);
    }

    @Override
    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {
        log.info("📱 TestServerMessageProcessorHandler 更新移动位置: {}", mobilePositionNotify);
    }

    @Override
    public void updateMediaStatus(MediaStatusNotify mediaStatusNotify) {
        log.info("📺 TestServerMessageProcessorHandler 更新媒体状态: {}", mediaStatusNotify);
    }

    @Override
    public void updateDeviceRecord(String userId, DeviceRecord deviceRecord) {
        log.info("📼 TestServerMessageProcessorHandler 更新设备录像: userId={}, record={}", userId, deviceRecord);
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {
        log.info("📋 TestServerMessageProcessorHandler 更新设备响应: userId={}, response={}", userId, deviceResponse);
    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {
        log.info("ℹ️ TestServerMessageProcessorHandler 更新设备信息: userId={}, info={}", userId, deviceInfo);
    }

    // === 测试辅助方法 ===

    /**
     * 重置测试状态
     */
    public static void resetTestState() {
        keepaliveLatch = new CountDownLatch(1);
        keepaliveReceived.set(false);
        receivedKeepalive.set(null);
        log.info("🔄 TestServerMessageProcessorHandler 测试状态已重置");
    }

    /**
     * 记录接收到的心跳
     */
    private static void recordKeepalive(DeviceKeepLiveNotify keepalive) {
        keepaliveReceived.set(true);
        receivedKeepalive.set(keepalive);
        if (keepaliveLatch != null) {
            keepaliveLatch.countDown();
        }
        log.info("📝 已记录心跳: {}", keepalive);
    }

    /**
     * 等待心跳接收
     */
    public static boolean waitForKeepalive(long timeout, TimeUnit unit) throws InterruptedException {
        if (keepaliveLatch == null) {
            return false;
        }
        return keepaliveLatch.await(timeout, unit);
    }

    /**
     * 检查是否接收到心跳
     */
    public static boolean hasReceivedKeepalive() {
        return keepaliveReceived.get();
    }

    /**
     * 获取接收到的心跳
     */
    public static DeviceKeepLiveNotify getReceivedKeepalive() {
        return receivedKeepalive.get();
    }
}