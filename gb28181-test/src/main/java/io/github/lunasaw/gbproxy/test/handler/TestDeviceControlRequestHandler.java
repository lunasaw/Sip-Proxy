package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control.DeviceControlRequestHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用于端到端测试DeviceControl命令的测试Handler，支持各类命令的回调、同步和断言
 */
@Component
public class TestDeviceControlRequestHandler implements DeviceControlRequestHandler {

    // PTZ
    private static CountDownLatch ptzLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlPtz> ptzCmdRef = new AtomicReference<>();

    // Guard
    private static CountDownLatch guardLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlGuard> guardCmdRef = new AtomicReference<>();

    // Alarm
    private static CountDownLatch alarmLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlAlarm> alarmCmdRef = new AtomicReference<>();

    // TeleBoot
    private static CountDownLatch teleBootLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlTeleBoot> teleBootCmdRef = new AtomicReference<>();

    // RecordCmd
    private static CountDownLatch recordLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlRecordCmd> recordCmdRef = new AtomicReference<>();

    // IFame
    private static CountDownLatch iFameLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlIFame> iFameCmdRef = new AtomicReference<>();

    // DragIn
    private static CountDownLatch dragInLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlDragIn> dragInCmdRef = new AtomicReference<>();

    // DragOut
    private static CountDownLatch dragOutLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlDragOut> dragOutCmdRef = new AtomicReference<>();

    // HomePosition
    private static CountDownLatch homePositionLatch = new CountDownLatch(1);
    private static final AtomicReference<DeviceControlPosition> homePositionCmdRef = new AtomicReference<>();

    // Handler实现
    @Override
    public void handlePtzCmd(DeviceControlPtz ptzCmd) {
        ptzCmdRef.set(ptzCmd);
        ptzLatch.countDown();
    }

    @Override
    public void handleGuardCmd(DeviceControlGuard guardCmd) {
        guardCmdRef.set(guardCmd);
        guardLatch.countDown();
    }

    @Override
    public void handleAlarmCmd(DeviceControlAlarm alarmCmd) {
        alarmCmdRef.set(alarmCmd);
        alarmLatch.countDown();
    }


    @Override
    public void handleTeleBoot(DeviceControlTeleBoot teleBootCmd) {
        teleBootCmdRef.set(teleBootCmd);
        teleBootLatch.countDown();
    }

    @Override
    public void handleRecordCmd(DeviceControlRecordCmd recordCmd) {
        recordCmdRef.set(recordCmd);
        recordLatch.countDown();
    }


    @Override
    public void handleIFameCmd(DeviceControlIFame iFameCmd) {
        iFameCmdRef.set(iFameCmd);
        iFameLatch.countDown();
    }

    @Override
    public void handleDragZoomIn(DeviceControlDragIn dragInCmd) {
        dragInCmdRef.set(dragInCmd);
        dragInLatch.countDown();
    }

    @Override
    public void handleDragZoomOut(DeviceControlDragOut dragOutCmd) {
        dragOutCmdRef.set(dragOutCmd);
        dragOutLatch.countDown();
    }

    @Override
    public void handleHomePosition(DeviceControlPosition homePositionCmd) {
        homePositionCmdRef.set(homePositionCmd);
        homePositionLatch.countDown();
    }

    // reset方法
    public static void resetTestState() {
        ptzLatch = new CountDownLatch(1);
        guardLatch = new CountDownLatch(1);
        alarmLatch = new CountDownLatch(1);
        teleBootLatch = new CountDownLatch(1);
        recordLatch = new CountDownLatch(1);
        iFameLatch = new CountDownLatch(1);
        dragInLatch = new CountDownLatch(1);
        dragOutLatch = new CountDownLatch(1);
        homePositionLatch = new CountDownLatch(1);

        ptzCmdRef.set(null);
        guardCmdRef.set(null);
        alarmCmdRef.set(null);
        teleBootCmdRef.set(null);
        recordCmdRef.set(null);
        iFameCmdRef.set(null);
        dragInCmdRef.set(null);
        dragOutCmdRef.set(null);
        homePositionCmdRef.set(null);
    }

    // 等待和断言方法（以PTZ为例，其他类似）
    public static boolean waitForPtz(long timeout, TimeUnit unit) throws InterruptedException {
        return ptzLatch.await(timeout, unit);
    }

    public static boolean hasReceivedPtz() {
        return ptzCmdRef.get() != null;
    }

    public static DeviceControlPtz getReceivedPtz() {
        return ptzCmdRef.get();
    }

    public static boolean waitForGuard(long timeout, TimeUnit unit) throws InterruptedException {
        return guardLatch.await(timeout, unit);
    }

    public static boolean hasReceivedGuard() {
        return guardCmdRef.get() != null;
    }

    public static DeviceControlGuard getReceivedGuard() {
        return guardCmdRef.get();
    }

    public static boolean waitForAlarm(long timeout, TimeUnit unit) throws InterruptedException {
        return alarmLatch.await(timeout, unit);
    }

    public static boolean hasReceivedAlarm() {
        return alarmCmdRef.get() != null;
    }

    public static DeviceControlAlarm getReceivedAlarm() {
        return alarmCmdRef.get();
    }

    public static boolean waitForTeleBoot(long timeout, TimeUnit unit) throws InterruptedException {
        return teleBootLatch.await(timeout, unit);
    }

    public static boolean hasReceivedTeleBoot() {
        return teleBootCmdRef.get() != null;
    }

    public static DeviceControlTeleBoot getReceivedTeleBoot() {
        return teleBootCmdRef.get();
    }

    public static boolean waitForRecord(long timeout, TimeUnit unit) throws InterruptedException {
        return recordLatch.await(timeout, unit);
    }

    public static boolean hasReceivedRecord() {
        return recordCmdRef.get() != null;
    }

    public static DeviceControlRecordCmd getReceivedRecord() {
        return recordCmdRef.get();
    }

    public static boolean waitForIFame(long timeout, TimeUnit unit) throws InterruptedException {
        return iFameLatch.await(timeout, unit);
    }

    public static boolean hasReceivedIFame() {
        return iFameCmdRef.get() != null;
    }

    public static DeviceControlIFame getReceivedIFame() {
        return iFameCmdRef.get();
    }

    public static boolean waitForDragIn(long timeout, TimeUnit unit) throws InterruptedException {
        return dragInLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDragIn() {
        return dragInCmdRef.get() != null;
    }

    public static DeviceControlDragIn getReceivedDragIn() {
        return dragInCmdRef.get();
    }

    public static boolean waitForDragOut(long timeout, TimeUnit unit) throws InterruptedException {
        return dragOutLatch.await(timeout, unit);
    }

    public static boolean hasReceivedDragOut() {
        return dragOutCmdRef.get() != null;
    }

    public static DeviceControlDragOut getReceivedDragOut() {
        return dragOutCmdRef.get();
    }

    public static boolean waitForHomePosition(long timeout, TimeUnit unit) throws InterruptedException {
        return homePositionLatch.await(timeout, unit);
    }

    public static boolean hasReceivedHomePosition() {
        return homePositionCmdRef.get() != null;
    }

    public static DeviceControlPosition getReceivedHomePosition() {
        return homePositionCmdRef.get();
    }
}