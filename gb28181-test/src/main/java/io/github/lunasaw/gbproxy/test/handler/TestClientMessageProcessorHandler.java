package io.github.lunasaw.gbproxy.test.handler;

import com.luna.common.date.DateUtils;
import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gbproxy.client.transmit.request.message.MessageRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 测试专用的Client消息处理器Handler
 * 用于验证客户端MESSAGE请求的处理流程
 */
@Component
@Slf4j
public class TestClientMessageProcessorHandler implements MessageRequestHandler {

    // === 测试辅助字段 ===
    private static CountDownLatch keepaliveLatch;
    private static AtomicBoolean keepaliveReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceKeepLiveNotify> receivedKeepalive = new AtomicReference<>();

    private static CountDownLatch alarmLatch;
    private static AtomicBoolean alarmReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceAlarmNotify> receivedAlarm = new AtomicReference<>();

    private static CountDownLatch deviceStatusLatch;
    private static AtomicBoolean deviceStatusReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceStatus> receivedDeviceStatus = new AtomicReference<>();

    private static CountDownLatch deviceInfoLatch;
    private static AtomicBoolean deviceInfoReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceInfo> receivedDeviceInfo = new AtomicReference<>();

    private static CountDownLatch deviceRecordLatch;
    private static AtomicBoolean deviceRecordReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceRecord> receivedDeviceRecord = new AtomicReference<>();

    private static CountDownLatch deviceConfigLatch;
    private static AtomicBoolean deviceConfigReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceConfigResponse> receivedDeviceConfig = new AtomicReference<>();

    // === Catalog 目录 ===
    private static CountDownLatch catalogLatch;
    private static AtomicBoolean catalogReceived = new AtomicBoolean(false);
    private static AtomicReference<DeviceResponse> receivedCatalog = new AtomicReference<>();

    // === MobilePosition 移动位置 ===
    private static CountDownLatch mobilePositionLatch;
    private static AtomicBoolean mobilePositionReceived = new AtomicBoolean(false);
    private static AtomicReference<MobilePositionNotify> receivedMobilePosition = new AtomicReference<>();

    // === PresetQuery 预置位 ===
    private static CountDownLatch presetQueryLatch;
    private static AtomicBoolean presetQueryReceived = new AtomicBoolean(false);
    private static AtomicReference<PresetQueryResponse> receivedPresetQuery = new AtomicReference<>();


    // === 业务方法实现 ===
    @Override
    public DeviceRecord getDeviceRecord(DeviceRecordQuery deviceRecordQuery) {
        log.info("[ClientTest] 获取设备录像信息: {}", deviceRecordQuery);
        DeviceRecord deviceRecord = new DeviceRecord();
        updateDeviceRecord(deviceRecord);
        return receivedDeviceRecord.get();
    }

    @Override
    public DeviceStatus getDeviceStatus(String userId) {
        log.info("[ClientTest] 获取设备状态信息: {}", userId);
        DeviceStatus deviceStatus = new DeviceStatus();
        updateDeviceStatus(deviceStatus);
        return receivedDeviceStatus.get();
    }

    @Override
    public DeviceInfo getDeviceInfo(String userId) {
        log.info("[ClientTest] 获取设备信息: {}", userId);
        DeviceInfo deviceInfo = new DeviceInfo();
        updateDeviceInfo(deviceInfo);
        return receivedDeviceInfo.get();
    }

    @Override
    public DeviceResponse getDeviceItem(String userId) {
        log.info("[ClientTest] 获取设备通道信息: {}", userId);
        DeviceResponse deviceResponse = new DeviceResponse();
        updateCatalog(deviceResponse);
        return receivedCatalog.get();
    }

    @Override
    public void broadcastNotify(DeviceBroadcastNotify broadcastNotify) {
        log.info("[ClientTest] 接收到语音广播通知: {}", broadcastNotify);
    }

    @Override
    public DeviceAlarmNotify getDeviceAlarmNotify(DeviceAlarmQuery deviceAlarmQuery) {
        log.info("[ClientTest] 获取设备告警通知: {}", deviceAlarmQuery);
        DeviceAlarmNotify alarmNotify = new DeviceAlarmNotify();
        updateDeviceAlarm(alarmNotify);
        return receivedAlarm.get();
    }

    @Override
    public DeviceConfigResponse getDeviceConfigResponse(DeviceConfigDownload deviceConfigDownload) {
        log.info("[ClientTest] 获取设备配置响应: {}", deviceConfigDownload);
        DeviceConfigResponse configResponse = new DeviceConfigResponse();
        updateDeviceConfig(configResponse);
        return receivedDeviceConfig.get();
    }

    @Override
    public <T> void deviceControl(T deviceControlBase) {
        log.info("[ClientTest] 处理设备控制命令: {}", deviceControlBase);
    }

    @Override
    public PresetQueryResponse getDevicePresetQueryResponse(PresetQuery presetQuery) {
        return null;
    }

    @Override
    public PresetQueryResponse getPresetQueryResponse(String userId) {
        log.info("[ClientTest] 获取设备预置位信息: {}", userId);
        PresetQueryResponse response = new PresetQueryResponse();
        updatePresetQuery(response);
        return receivedPresetQuery.get();
    }

    public void updatePresetQuery(PresetQueryResponse response) {
        log.info("[ClientTest] 更新设备预置位: {}", response);
        presetQueryReceived.set(true);
        receivedPresetQuery.set(response);
        if (presetQueryLatch != null) presetQueryLatch.countDown();
    }

    public static boolean waitForPresetQuery(long timeout, TimeUnit unit) throws InterruptedException {
        if (presetQueryLatch == null) return false;
        return presetQueryLatch.await(timeout, unit);
    }

    public static boolean hasReceivedPresetQuery() {
        return presetQueryReceived.get();
    }

    public static PresetQueryResponse getReceivedPresetQuery() {
        return receivedPresetQuery.get();
    }

    @Override
    public ConfigDownloadResponse getConfigDownloadResponse(String userId, String configType) {
        log.info("[ClientTest] 获取设备配置查询应答: {}, configType={}", userId, configType);
        ConfigDownloadResponse response = new ConfigDownloadResponse();
        // 可根据需要填充模拟数据
        // response.setBasicParam(...);
        // 可添加辅助测试状态
        return response;
    }

    @Override
    public MobilePositionNotify getMobilePositionNotify(MobilePositionQuery mobilePositionQuery) {
        // 模拟获取移动位置通知
        MobilePositionNotify mobilePositionNotify = new MobilePositionNotify();
        mobilePositionNotify.setDeviceId(mobilePositionQuery.getDeviceId());
        mobilePositionNotify.setLatitude("34.0522");
        mobilePositionNotify.setLongitude("-118.2437");
        mobilePositionNotify.setTime(DateUtils.formatDateTime(new Date()));
        mobilePositionNotify.setSpeed("10.0");
        log.info("[ClientTest] 获取设备移动位置通知: {}", mobilePositionQuery);
        updateMobilePosition(mobilePositionNotify);
        return mobilePositionNotify;
    }

    public void updateMobilePosition(MobilePositionNotify mobilePositionNotify) {
        log.info("[ClientTest] 更新移动位置: {}", mobilePositionNotify);
        mobilePositionReceived.set(true);
        receivedMobilePosition.set(mobilePositionNotify);
        if (mobilePositionLatch != null) mobilePositionLatch.countDown();
    }

    public void keepLiveDevice(DeviceKeepLiveNotify deviceKeepLiveNotify) {
        log.info("[ClientTest] 接收到心跳: {}", deviceKeepLiveNotify);
        keepaliveReceived.set(true);
        receivedKeepalive.set(deviceKeepLiveNotify);
        if (keepaliveLatch != null) keepaliveLatch.countDown();
    }

    public void updateDeviceAlarm(DeviceAlarmNotify deviceAlarmNotify) {
        log.info("[ClientTest] 更新设备报警: {}", deviceAlarmNotify);
        alarmReceived.set(true);
        receivedAlarm.set(deviceAlarmNotify);
        if (alarmLatch != null) alarmLatch.countDown();
    }

    public void updateMediaStatus(MediaStatusNotify mediaStatusNotify) {
        log.info("[ClientTest] 更新媒体状态: {}", mediaStatusNotify);
    }

    // === 设备状态 ===
    public void updateDeviceStatus(DeviceStatus deviceStatus) {
        log.info("[ClientTest] 更新设备状态: {}", deviceStatus);
        deviceStatusReceived.set(true);
        receivedDeviceStatus.set(deviceStatus);
        if (deviceStatusLatch != null) deviceStatusLatch.countDown();
    }

    // === 设备信息 ===
    public void updateDeviceInfo(DeviceInfo deviceInfo) {
        log.info("[ClientTest] 更新设备信息: {}", deviceInfo);
        deviceInfoReceived.set(true);
        receivedDeviceInfo.set(deviceInfo);
        if (deviceInfoLatch != null) deviceInfoLatch.countDown();
    }

    // === 设备录像 ===
    public void updateDeviceRecord(DeviceRecord deviceRecord) {
        log.info("[ClientTest] 更新设备录像: {}", deviceRecord);
        deviceRecordReceived.set(true);
        receivedDeviceRecord.set(deviceRecord);
        if (deviceRecordLatch != null) deviceRecordLatch.countDown();
    }

    // === 设备配置 ===
    public void updateDeviceConfig(DeviceConfigResponse deviceConfigResponse) {
        log.info("[ClientTest] 更新设备配置: {}", deviceConfigResponse);
        deviceConfigReceived.set(true);
        receivedDeviceConfig.set(deviceConfigResponse);
        if (deviceConfigLatch != null) deviceConfigLatch.countDown();
    }

    // === Catalog 目录 ===
    public void updateCatalog(DeviceResponse catalog) {
        log.info("[ClientTest] 更新设备目录: {}", catalog);
        catalogReceived.set(true);
        receivedCatalog.set(catalog);
        if (catalogLatch != null) catalogLatch.countDown();
    }

    // === MobilePosition 移动位置 ===
    public static boolean waitForMobilePosition(long timeout, TimeUnit unit) throws InterruptedException {
        if (mobilePositionLatch == null) return false;
        return mobilePositionLatch.await(timeout, unit);
    }

    public static boolean hasReceivedMobilePosition() {
        return mobilePositionReceived.get();
    }

    public static MobilePositionNotify getReceivedMobilePosition() {
        return receivedMobilePosition.get();
    }

    // === 测试辅助方法 ===
    public static void resetTestState() {
        keepaliveLatch = new CountDownLatch(1);
        keepaliveReceived.set(false);
        receivedKeepalive.set(null);
        alarmLatch = new CountDownLatch(1);
        alarmReceived.set(false);
        receivedAlarm.set(null);
        deviceStatusLatch = new CountDownLatch(1);
        deviceStatusReceived.set(false);
        receivedDeviceStatus.set(null);
        deviceInfoLatch = new CountDownLatch(1);
        deviceInfoReceived.set(false);
        receivedDeviceInfo.set(null);
        deviceRecordLatch = new CountDownLatch(1);
        deviceRecordReceived.set(false);
        receivedDeviceRecord.set(null);
        deviceConfigLatch = new CountDownLatch(1);
        deviceConfigReceived.set(false);
        receivedDeviceConfig.set(null);
        catalogLatch = new CountDownLatch(1);
        catalogReceived.set(false);
        receivedCatalog.set(null);
        mobilePositionLatch = new CountDownLatch(1);
        mobilePositionReceived.set(false);
        receivedMobilePosition.set(null);
        presetQueryLatch = new CountDownLatch(1);
        presetQueryReceived.set(false);
        receivedPresetQuery.set(null);
        log.info("[ClientTest] 测试状态已重置");
    }

    // === 等待与断言方法 ===
    public static boolean waitForKeepalive(long timeout, TimeUnit unit) throws InterruptedException {
        if (keepaliveLatch == null) return false;
        return keepaliveLatch.await(timeout, unit);
    }

    public static boolean hasReceivedKeepalive() {
        return keepaliveReceived.get();
    }

    public static DeviceKeepLiveNotify getReceivedKeepalive() {
        return receivedKeepalive.get();
    }

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

    public static boolean waitForDeviceStatus(long timeout, TimeUnit unit) throws InterruptedException {
        if (deviceStatusLatch == null) return false;
        return deviceStatusLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDeviceStatus() {
        return deviceStatusReceived.get();
    }

    public static DeviceStatus getReceivedDeviceStatus() {
        return receivedDeviceStatus.get();
    }

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
}