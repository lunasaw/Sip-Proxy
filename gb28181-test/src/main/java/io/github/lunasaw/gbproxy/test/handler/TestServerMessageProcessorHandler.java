package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gbproxy.server.transmit.request.message.ServerMessageProcessorHandler;
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

    // === 新增：报警 ===
    private static CountDownLatch alarmLatch;
    private static AtomicBoolean alarmReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceAlarmNotify> receivedAlarm = new AtomicReference<>();

    // === 新增：目录 ===
    private static CountDownLatch catalogLatch;
    private static AtomicBoolean catalogReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceResponse> receivedCatalog = new AtomicReference<>();

    // === 新增：设备信息 ===
    private static CountDownLatch deviceInfoLatch;
    private static AtomicBoolean deviceInfoReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceInfo> receivedDeviceInfo = new AtomicReference<>();

    // === 新增：设备状态 ===
    private static CountDownLatch deviceStatusLatch;
    private static AtomicBoolean deviceStatusReceived = new AtomicBoolean(false);
    private static AtomicReference<io.github.lunasaw.gb28181.common.entity.response.DeviceStatus> receivedDeviceStatus = new AtomicReference<>();

    // === 新增：录像 ===
    private static CountDownLatch deviceRecordLatch;
    private static AtomicBoolean deviceRecordReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceRecord> receivedDeviceRecord = new AtomicReference<>();

    // === 新增：设备配置 ===
    private static CountDownLatch deviceConfigLatch;
    private static AtomicBoolean deviceConfigReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceConfigResponse> receivedDeviceConfig = new AtomicReference<>();

    // === 新增：Invite/Play 点播 ===
    private static CountDownLatch invitePlayLatch;
    private static AtomicBoolean invitePlayReceived = new AtomicBoolean(false);
    private static AtomicReference<String> receivedInvitePlayCallId = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlaySdp = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlayFromUserId = new AtomicReference<>();

    // === 新增：Invite/PlayBack 回放 ===
    private static CountDownLatch invitePlayBackLatch;
    private static AtomicBoolean invitePlayBackReceived = new AtomicBoolean(false);
    private static AtomicReference<String> receivedInvitePlayBackCallId = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlayBackSdp = new AtomicReference<>();
    private static AtomicReference<String> receivedInvitePlayBackFromUserId = new AtomicReference<>();

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
        alarmReceived.set(true);
        receivedAlarm.set(deviceAlarmNotify);
        if (alarmLatch != null) alarmLatch.countDown();
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
        deviceRecordReceived.set(true);
        receivedDeviceRecord.set(deviceRecord);
        if (deviceRecordLatch != null) deviceRecordLatch.countDown();
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse deviceResponse) {
        log.info("📋 TestServerMessageProcessorHandler 更新设备响应: userId={}, response={}", userId, deviceResponse);
        catalogReceived.set(true);
        receivedCatalog.set(deviceResponse);
        if (catalogLatch != null) catalogLatch.countDown();
    }

    @Override
    public void updateDeviceInfo(String userId, DeviceInfo deviceInfo) {
        log.info("ℹ️ TestServerMessageProcessorHandler 更新设备信息: userId={}, info={}", userId, deviceInfo);
        deviceInfoReceived.set(true);
        receivedDeviceInfo.set(deviceInfo);
        if (deviceInfoLatch != null) deviceInfoLatch.countDown();
    }

    @Override
    public void updateDeviceConfig(String userId, DeviceConfigResponse deviceConfigResponse) {
        log.info("⚙️ TestServerMessageProcessorHandler 更新设备配置: userId={}, config={}", userId, deviceConfigResponse);
        deviceConfigReceived.set(true);
        receivedDeviceConfig.set(deviceConfigResponse);
        if (deviceConfigLatch != null) deviceConfigLatch.countDown();
    }

    // 设备状态
    public void updateDeviceStatus(String userId, io.github.lunasaw.gb28181.common.entity.response.DeviceStatus deviceStatus) {
        log.info("📶 TestServerMessageProcessorHandler 更新设备状态: userId={}, status={}", userId, deviceStatus);
        deviceStatusReceived.set(true);
        receivedDeviceStatus.set(deviceStatus);
        if (deviceStatusLatch != null) deviceStatusLatch.countDown();
    }

    // === 测试辅助方法 ===
    public static void resetTestState() {
        keepaliveLatch = new CountDownLatch(1);
        keepaliveReceived.set(false);
        receivedKeepalive.set(null);
        alarmLatch = new CountDownLatch(1);
        alarmReceived.set(false);
        receivedAlarm.set(null);
        catalogLatch = new CountDownLatch(1);
        catalogReceived.set(false);
        receivedCatalog.set(null);
        deviceInfoLatch = new CountDownLatch(1);
        deviceInfoReceived.set(false);
        receivedDeviceInfo.set(null);
        deviceStatusLatch = new CountDownLatch(1);
        deviceStatusReceived.set(false);
        receivedDeviceStatus.set(null);
        deviceRecordLatch = new CountDownLatch(1);
        deviceRecordReceived.set(false);
        receivedDeviceRecord.set(null);
        deviceConfigLatch = new CountDownLatch(1);
        deviceConfigReceived.set(false);
        receivedDeviceConfig.set(null);
        invitePlayLatch = new CountDownLatch(1);
        invitePlayReceived.set(false);
        receivedInvitePlayCallId.set(null);
        receivedInvitePlaySdp.set(null);
        receivedInvitePlayFromUserId.set(null);
        invitePlayBackLatch = new CountDownLatch(1);
        invitePlayBackReceived.set(false);
        receivedInvitePlayBackCallId.set(null);
        receivedInvitePlayBackSdp.set(null);
        receivedInvitePlayBackFromUserId.set(null);
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

    // === 报警 ===
    public static boolean waitForAlarm(long timeout, TimeUnit unit) throws InterruptedException {
        if (alarmLatch == null) return false;
        return alarmLatch.await(timeout, unit);
    }

    public static boolean hasReceivedAlarm() {
        return alarmReceived.get();
    }

    public static DeviceAlarmNotify getReceivedAlarm() {
        return receivedAlarm.get();
    }

    // === 目录 ===
    public static boolean waitForCatalog(long timeout, TimeUnit unit) throws InterruptedException {
        if (catalogLatch == null) return false;
        return catalogLatch.await(timeout, unit);
    }

    public static boolean hasReceivedCatalog() {
        return catalogReceived.get();
    }

    public static DeviceResponse getReceivedCatalog() {
        return receivedCatalog.get();
    }

    // === 设备信息 ===
    public static boolean waitForDeviceInfo(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceInfoLatch == null) return false;
        return deviceInfoLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceInfo() {
        return deviceInfoReceived.get();
    }

    public static DeviceInfo getReceivedDeviceInfo() {
        return receivedDeviceInfo.get();
    }

    // === 设备状态 ===
    public static boolean waitForDeviceStatus(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceStatusLatch == null) return false;
        return deviceStatusLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceStatus() {
        return deviceStatusReceived.get();
    }

    public static io.github.lunasaw.gb28181.common.entity.response.DeviceStatus getReceivedDeviceStatus() {
        return receivedDeviceStatus.get();
    }

    // === 录像 ===
    public static boolean waitForDeviceRecord(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceRecordLatch == null) return false;
        return deviceRecordLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceRecord() {
        return deviceRecordReceived.get();
    }

    public static DeviceRecord getReceivedDeviceRecord() {
        return receivedDeviceRecord.get();
    }

    // === 设备配置 ===
    public static boolean waitForDeviceConfig(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceConfigLatch == null) return false;
        return deviceConfigLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceConfig() {
        return deviceConfigReceived.get();
    }

    public static DeviceConfigResponse getReceivedDeviceConfig() {
        return receivedDeviceConfig.get();
    }

    // === Invite/Play 点播 ===
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

    public static String getReceivedInvitePlayFromUserId() {
        return receivedInvitePlayFromUserId.get();
    }

    // === Invite/PlayBack 回放 ===
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

    public static String getReceivedInvitePlayBackFromUserId() {
        return receivedInvitePlayBackFromUserId.get();
    }

    // === Invite/Play 点播更新方法 ===
    public static void updateInvitePlay(String callId, String sdpContent, String fromUserId) {
        log.info("📺 TestServerMessageProcessorHandler 更新实时点播: callId={}, fromUserId={}, sdp={}", callId, fromUserId, sdpContent);
        invitePlayReceived.set(true);
        receivedInvitePlayCallId.set(callId);
        receivedInvitePlaySdp.set(sdpContent);
        receivedInvitePlayFromUserId.set(fromUserId);
        if (invitePlayLatch != null) invitePlayLatch.countDown();
    }

    public static void updateInvitePlayBack(String callId, String sdpContent, String fromUserId) {
        log.info("📼 TestServerMessageProcessorHandler 更新回放点播: callId={}, fromUserId={}, sdp={}", callId, fromUserId, sdpContent);
        invitePlayBackReceived.set(true);
        receivedInvitePlayBackCallId.set(callId);
        receivedInvitePlayBackSdp.set(sdpContent);
        receivedInvitePlayBackFromUserId.set(fromUserId);
        if (invitePlayBackLatch != null) invitePlayBackLatch.countDown();
    }
}