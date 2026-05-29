package io.github.lunasaw.gbproxy.server.transmit.cmd;

import com.luna.common.check.Assert;
import com.luna.common.date.DateUtils;
import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandStrategyFactory;
import io.github.lunasaw.gbproxy.server.entity.InviteRequest;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.SipSender;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.address.SipURI;
import java.util.Date;

/**
 * 平台服务端出向命令发送器，封装所有平台 → 设备方向的 SIP 指令。
 *
 * <p>调用方只需传 {@code deviceId}，内部通过 {@link DeviceSessionCache} 查找设备寻址信息，
 * 再经 {@link io.github.lunasaw.gb28181.common.transmit.cmd.CommandStrategyFactory} 路由到对应策略执行。
 *
 * <p>1.7.0 起 BYE/SUBSCRIBE 刷新/退订均为 dialog-aware，需传 {@code callId} 而非 {@code deviceId}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerCommandSender {

    private final CommandStrategyFactory factory;
    private final ServerDeviceSupplier deviceSupplier;
    private final DeviceSessionCache sessionCache;

    // ==================== 实例方法（新 API） ====================

    /**
     * 发送设备信息查询（cmdType=DeviceInfo）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceInfoQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), sn(), deviceId));
    }

    /**
     * 发送设备状态查询（cmdType=DeviceStatus）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceStatusQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), sn(), deviceId));
    }

    /**
     * 发送设备目录查询（cmdType=Catalog）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceCatalogQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), deviceId));
    }

    /**
     * 发送预置位查询（cmdType=PresetQuery）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String devicePresetQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), sn(), deviceId));
    }

    /**
     * 发送录像信息查询（cmdType=RecordInfo，时间参数为字符串格式）。
     *
     * @param deviceId  目标设备 ID
     * @param startTime 查询起始时间（ISO 8601 格式）
     * @param endTime   查询结束时间（ISO 8601 格式）
     * @return SIP Call-ID
     */
    public String deviceRecordInfoQuery(String deviceId, String startTime, String endTime) {
        DeviceRecordQuery q = new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), sn(), deviceId);
        q.setStartTime(startTime);
        q.setEndTime(endTime);
        return send("MESSAGE", deviceId, q);
    }

    /**
     * 发送录像信息查询（cmdType=RecordInfo，时间参数为毫秒时间戳）。
     *
     * @param deviceId  目标设备 ID
     * @param startTime 查询起始时间（毫秒时间戳）
     * @param endTime   查询结束时间（毫秒时间戳）
     * @return SIP Call-ID
     */
    public String deviceRecordInfoQuery(String deviceId, long startTime, long endTime) {
        return deviceRecordInfoQuery(deviceId,
            DateUtils.formatDateTime(new Date(startTime)),
            DateUtils.formatDateTime(new Date(endTime)));
    }

    /**
     * 发送移动位置查询（cmdType=MobilePosition）。
     *
     * @param deviceId 目标设备 ID
     * @param interval 上报间隔（秒）
     * @return SIP Call-ID
     */
    public String deviceMobilePositionQuery(String deviceId, String interval) {
        DeviceMobileQuery q = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), sn(), deviceId);
        q.setInterval(interval);
        return send("MESSAGE", deviceId, q);
    }

    /**
     * 发送目录订阅（SUBSCRIBE Catalog）。
     *
     * @param deviceId  目标设备 ID
     * @param expires   订阅有效期（秒），0 表示退订
     * @param eventType 事件类型，固定 "Catalog"
     * @return SIP Call-ID
     */
    public String deviceCatalogSubscribe(String deviceId, Integer expires, String eventType) {
        ToDevice to = getToDevice(deviceId);
        to.setExpires(expires);
        to.setEventType(eventType);
        FromDevice from = deviceSupplier.getServerFromDevice();
        DeviceQuery body = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), deviceId);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return factory.getStrategy("server", "SUBSCRIBE")
            .execute(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
                .toBuilder().body(body).build());
    }

    /**
     * 发送移动位置订阅（SUBSCRIBE MobilePosition）。
     *
     * @param deviceId  目标设备 ID
     * @param interval  上报间隔（秒）
     * @param expires   订阅有效期（秒），0 表示退订
     * @param eventType 事件类型，固定 "MobilePosition"
     * @param eventId   事件 ID
     * @return SIP Call-ID
     */
    public String deviceMobilePositionSubscribe(String deviceId, String interval,
                                                 Integer expires, String eventType, String eventId) {
        ToDevice to = getToDevice(deviceId);
        to.setExpires(expires);
        to.setEventType(eventType);
        to.setEventId(eventId);
        FromDevice from = deviceSupplier.getServerFromDevice();
        DeviceMobileQuery body = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), sn(), deviceId);
        body.setInterval(interval);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return factory.getStrategy("server", "SUBSCRIBE")
            .execute(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
                .toBuilder().body(body).build());
    }

    /**
     * GB28181-2022 §9.11.1 / A.2.4.6 报警事件订阅 (SUBSCRIBE Alarm)
     *
     * @param expires 订阅有效期（秒），传 0 表示退订
     * @param eventType 事件类型，固定 "Alarm"
     * @param startPriority 起始报警等级（可选，传 null 跳过）
     * @param endPriority 结束报警等级（可选）
     * @param alarmMethod 报警方式（可选）
     * @param alarmType 报警类型（可选）
     * @param startAlarmTime 起始时间 ISO8601 格式（可选）
     * @param endAlarmTime 结束时间 ISO8601 格式（可选）
     */
    public String deviceAlarmSubscribe(String deviceId, Integer expires, String eventType,
                                        String startPriority, String endPriority,
                                        String alarmMethod, String alarmType,
                                        String startAlarmTime, String endAlarmTime) {
        ToDevice to = getToDevice(deviceId);
        to.setExpires(expires);
        to.setEventType(eventType);
        String eventId = sn();
        to.setEventId(eventId);
        FromDevice from = deviceSupplier.getServerFromDevice();
        DeviceAlarmQuery body = new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), sn(), deviceId);
        body.setStartAlarmPriority(startPriority);
        body.setEndAlarmPriority(endPriority);
        body.setAlarmMethod(alarmMethod);
        body.setAlarmType(alarmType);
        body.setStartTime(startAlarmTime);
        body.setEndTime(endAlarmTime);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        subscribeInfo.setEventId(eventId);
        return factory.getStrategy("server", "SUBSCRIBE")
            .execute(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
                .toBuilder().body(body).build());
    }

    /**
     * GB28181-2022 §9.11.1 / §A.2.4.13 PTZ 精准位置变化事件订阅。
     *
     * <p>事件源（设备）会按 SubscribeInfo 中的间隔向事件观察者（平台）NOTIFY PTZ 位置变化，
     * 直到订阅过期。订阅成功后由 {@code SubscribePtzPositionQueryMessageHandler} 在 client 侧
     * 接收并通过 {@code SubscribeListener.onPtzPositionSubscribe} 回调业务方。
     *
     * @param deviceId 目标设备 ID
     * @param expires  订阅过期时间（秒）；0 表示取消订阅
     */
    public String devicePtzPositionSubscribe(String deviceId, Integer expires) {
        ToDevice to = getToDevice(deviceId);
        to.setExpires(expires);
        String eventId = sn();
        to.setEventId(eventId);
        FromDevice from = deviceSupplier.getServerFromDevice();
        PTZPositionQuery body = new PTZPositionQuery(CmdTypeEnum.PTZ_POSITION.getType(), sn(), deviceId);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setExpires(expires);
        subscribeInfo.setEventId(eventId);
        return factory.getStrategy("server", "SUBSCRIBE")
            .execute(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
                .toBuilder().body(body).build());
    }

    /**
     * 发送报警查询（cmdType=Alarm）。
     *
     * @param deviceId      目标设备 ID
     * @param startTime     查询起始时间
     * @param endTime       查询结束时间
     * @param startPriority 起始报警等级（可选）
     * @param endPriority   结束报警等级（可选）
     * @param alarmMethod   报警方式（可选）
     * @param alarmType     报警类型（可选）
     * @return SIP Call-ID
     */
    public String deviceAlarmQuery(String deviceId, Date startTime, Date endTime,
                                    String startPriority, String endPriority,
                                    String alarmMethod, String alarmType) {
        DeviceAlarmQuery q = new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), sn(), deviceId);
        q.setStartTime(DateUtils.formatDateTime(startTime));
        q.setEndTime(DateUtils.formatDateTime(endTime));
        q.setStartAlarmPriority(startPriority);
        q.setEndAlarmPriority(endPriority);
        q.setAlarmMethod(alarmMethod);
        q.setAlarmType(alarmType);
        return send("MESSAGE", deviceId, q);
    }

    /**
     * 发送布防/撤防控制（GuardCmd）。
     *
     * @param deviceId     目标设备 ID
     * @param guardCmdStr  布防命令，取值 "SetGuard"（布防）或 "ResetGuard"（撤防）
     * @return SIP Call-ID
     */
    public String deviceControlGuardCmd(String deviceId, String guardCmdStr) {
        DeviceControlGuard g = new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        g.setGuardCmd(guardCmdStr);
        return send("MESSAGE", deviceId, g);
    }

    /**
     * 发送报警复位（AlarmCmd=ResetAlarm，简化重载）。
     *
     * @param deviceId    目标设备 ID
     * @param alarmMethod 报警方式
     * @param alarmType   报警类型
     * @return SIP Call-ID
     */
    public String deviceControlAlarm(String deviceId, String alarmMethod, String alarmType) {
        return deviceControlAlarm(deviceId, "ResetAlarm", alarmMethod, alarmType);
    }

    /**
     * GBT-28181-2022 §A.2.3.1.6 报警复位（AlarmCmd）。
     *
     * @param alarmCmd  报警命令文本，标准取值 "ResetAlarm"
     * @param alarmMethod 报警方式
     * @param alarmType   报警类型
     */
    public String deviceControlAlarm(String deviceId, String alarmCmd, String alarmMethod, String alarmType) {
        DeviceControlAlarm a = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        a.setAlarmCmd(alarmCmd);
        DeviceControlAlarm.AlarmInfo info = new DeviceControlAlarm.AlarmInfo();
        info.setAlarmMethod(alarmMethod);
        info.setAlarmType(alarmType);
        a.setAlarmInfo(info);
        return send("MESSAGE", deviceId, a);
    }

    /**
     * GBT-28181-2022 §A.3.2 PTZ 控制（云台 + 变倍，单方向）。
     *
     * @param control PTZ 控制类型
     * @param speed   水平/垂直/变倍统一速度（0-255，变倍取低 4 位）
     */
    public String deviceControlPtzCmd(String deviceId,
                                       io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum control,
                                       int speed) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addPTZControl(control)
            .horizontalSpeed(Math.min(speed, 0xFF))
            .verticalSpeed(Math.min(speed, 0xFF))
            .zoomSpeed(Math.min(speed, 0xF))
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * 发送 PTZ 控制指令（原始十六进制字符串）。
     *
     * @param deviceId 目标设备 ID
     * @param ptzCmd   PTZ 控制字节串（十六进制，8 字节）
     * @return SIP Call-ID
     */
    public String deviceControlPtzCmd(String deviceId, String ptzCmd) {
        DeviceControlPtz p = new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        p.setPtzCmd(ptzCmd);
        p.setPtzInfo(new DeviceControlPtz.PtzInfo());
        return send("MESSAGE", deviceId, p);
    }

    /**
     * GBT-28181-2022 §A.3.3 FI 控制（光圈 + 聚焦）。
     */
    public String deviceControlFI(String deviceId,
                                   io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum.IrisDirection iris,
                                   io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum.FocusDirection focus,
                                   int focusSpeed, int irisSpeed) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addFIControl(iris, focus)
            .focusSpeed(focusSpeed)
            .irisSpeed(irisSpeed)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * GBT-28181-2022 §A.3.4 预置位（设置/调用/删除）。
     *
     * @param presetNumber 预置位号 1-255（0 号预留）
     */
    public String deviceControlPreset(String deviceId,
                                       io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PresetControlEnum action,
                                       int presetNumber) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addPresetControl(action, presetNumber)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * GBT-28181-2022 §A.3.5 巡航（加入巡航点 / 设置速度 / 设置停留时间 / 开始巡航 / 删除巡航点）。
     *
     * @param groupNumber  巡航组号 0-255
     * @param presetNumber 预置位号 1-255（删除整条巡航时传 0）
     */
    public String deviceControlCruise(String deviceId,
                                       io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum action,
                                       int groupNumber, int presetNumber) {
        io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder builder =
            io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
                .address(0x001);
        if (presetNumber == 0 && action == io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum.DELETE_CRUISE_POINT) {
            // 删除整条巡航：字节 6 = 00H
            builder.addCruiseControl(action, groupNumber);
        } else {
            builder.addCruiseControl(action, groupNumber, presetNumber);
        }
        return deviceControlPtzCmd(deviceId, builder.buildToHex());
    }

    /**
     * GBT-28181-2022 §A.3.5 巡航速度/停留时间设置（数据范围 0-4095，跨字节 6+7 高 4 位）。
     */
    public String deviceControlCruiseSpeedOrTime(String deviceId,
                                                  io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum action,
                                                  int groupNumber, int presetNumber, int speedOrTime) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addCruiseControl(action, groupNumber, presetNumber, speedOrTime)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * GBT-28181-2022 §A.3.6 扫描（开始/设置左边界/设置右边界）。
     */
    public String deviceControlScan(String deviceId, int groupNumber,
                                     io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum.ScanOperationType operationType) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addScanControl(io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum.START_AUTO_SCAN,
                groupNumber, operationType)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * GBT-28181-2022 §A.3.6 设置自动扫描速度（数据范围 0-4095）。
     */
    public String deviceControlScanSpeed(String deviceId, int groupNumber, int speed) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addScanSpeedControl(groupNumber, speed)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * GBT-28181-2022 §A.3.7 辅助开关（开/关，switchNumber=1 表示雨刷）。
     */
    public String deviceControlAuxiliary(String deviceId,
                                          io.github.lunasaw.gb28181.common.entity.control.instruction.enums.AuxiliaryControlEnum action,
                                          int switchNumber) {
        String hex = io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder.create()
            .address(0x001)
            .addAuxiliaryControl(action, switchNumber)
            .buildToHex();
        return deviceControlPtzCmd(deviceId, hex);
    }

    /**
     * 发送设备重启控制（TeleBoot）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceControlReboot(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId));
    }

    /**
     * 发送录像控制（RecordCmd）。
     *
     * @param deviceId  目标设备 ID
     * @param recordCmd 录像命令，取值 "Record"（开始录像）或 "StopRecord"（停止录像）
     * @return SIP Call-ID
     */
    public String deviceControlRecord(String deviceId, String recordCmd) {
        DeviceControlRecordCmd r = new DeviceControlRecordCmd(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        r.setRecordCmd(recordCmd);
        return send("MESSAGE", deviceId, r);
    }

    /**
     * GB28181-2022 A.2.3.1.12 设备软件升级控制
     */
    public String deviceUpgrade(String deviceId, String firmware, String fileURL, String manufacturer, String sessionId) {
        DeviceUpgradeControl control = new DeviceUpgradeControl(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        control.setDeviceUpgrade(new DeviceUpgradeControl.DeviceUpgrade(firmware, fileURL, manufacturer, sessionId));
        return send("MESSAGE", deviceId, control);
    }

    /**
     * GB28181-2022 A.2.3.2.12 图像抓拍配置（cmdType=DeviceConfig，子标签 SnapShotConfig）
     */
    public String deviceSnapShot(String deviceId, Integer snapNum, Integer interval, String uploadURL, String sessionId) {
        SnapShotConfig config = new SnapShotConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        config.setSnapShotConfig(new SnapShotConfig.SnapShotInfo(snapNum, interval, uploadURL, sessionId));
        return send("MESSAGE", deviceId, config);
    }

    /**
     * GB28181-2022 A.2.3.1.11 PTZ 精准控制
     */
    public String deviceControlPtzPrecise(String deviceId, Double pan, Double tilt, Double zoom) {
        DeviceControlPTZPrecise precise = new DeviceControlPTZPrecise(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        precise.setPtzPreciseCtrl(new DeviceControlPTZPrecise.PTZPreciseCtrl(pan, tilt, zoom));
        return send("MESSAGE", deviceId, precise);
    }

    /**
     * GB28181-2022 A.2.4.13 PTZ 精确状态查询
     */
    public String devicePtzPositionQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new PTZPositionQuery(CmdTypeEnum.PTZ_POSITION.getType(), sn(), deviceId));
    }

    /**
     * GB28181-2022 A.2.4.14 存储卡状态查询
     */
    public String deviceSdCardStatusQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new SDCardStatusQuery(CmdTypeEnum.SD_CARD_STATUS.getType(), sn(), deviceId));
    }

    /**
     * GB28181-2022 A.2.3.1.13 存储卡格式化控制
     *
     * @param sdNumber SD 卡编号，从 1 开始；0 表示格式化所有存储卡
     */
    public String deviceControlFormatSDCard(String deviceId, Integer sdNumber) {
        DeviceControlSDCardFormat format = new DeviceControlSDCardFormat(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        format.setFormatSDCard(sdNumber);
        return send("MESSAGE", deviceId, format);
    }

    /**
     * GB28181-2022 A.2.3.1.10 看守位控制（设置 / 调用看守位）
     *
     * @param enabled 看守位开关："0"-关闭，"1"-开启
     * @param resetTime 自动归位时间间隔（秒），可选
     * @param presetIndex 预置位编号（0-255），可选
     */
    public String deviceControlHomePosition(String deviceId, String enabled, String resetTime, String presetIndex) {
        DeviceControlPosition control = new DeviceControlPosition(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        DeviceControlPosition.HomePosition info = new DeviceControlPosition.HomePosition(enabled, resetTime, presetIndex);
        control.setHomePosition(info);
        return send("MESSAGE", deviceId, control);
    }

    /**
     * GB28181-2022 A.2.4.10 看守位信息查询
     */
    public String deviceHomePositionQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new HomePositionQuery(CmdTypeEnum.HOME_POSITION_QUERY.getType(), sn(), deviceId));
    }

    /**
     * GB28181-2022 A.2.4.11 巡航轨迹列表查询
     */
    public String deviceCruiseTrackListQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new CruiseTrackListQuery(CmdTypeEnum.CRUISE_TRACK_LIST_QUERY.getType(), sn(), deviceId));
    }

    /**
     * GB28181-2022 A.2.4.12 巡航轨迹查询（指定轨迹编号）
     */
    public String deviceCruiseTrackQuery(String deviceId, Integer number) {
        CruiseTrackQuery q = new CruiseTrackQuery(CmdTypeEnum.CRUISE_TRACK_QUERY.getType(), sn(), deviceId);
        q.setNumber(number);
        return send("MESSAGE", deviceId, q);
    }

    /**
     * GB28181-2022 A.2.3.1.7 强制关键帧
     */
    public String deviceControlIFrame(String deviceId) {
        DeviceControlIFame ifame = new DeviceControlIFame(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        ifame.setIFameCmd("Send");
        return send("MESSAGE", deviceId, ifame);
    }

    /**
     * GB28181-2022 A.2.3.1.8 拉框放大
     */
    public String deviceControlDragZoomIn(String deviceId, DragZoom dragZoom) {
        DeviceControlDragIn drag = new DeviceControlDragIn(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        drag.setDragZoomIn(dragZoom);
        return send("MESSAGE", deviceId, drag);
    }

    /**
     * GB28181-2022 A.2.3.1.9 拉框缩小
     */
    public String deviceControlDragZoomOut(String deviceId, DragZoom dragZoom) {
        DeviceControlDragOut drag = new DeviceControlDragOut(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        drag.setDragZoomOut(dragZoom);
        return send("MESSAGE", deviceId, drag);
    }

    /**
     * GB28181-2022 A.2.3.1.14 目标跟踪
     *
     * @param mode "Auto" / "Manual" / "Stop"
     * @param deviceId2 全景相机的全景通道 ID（手动跟踪时必选）
     * @param targetArea 目标框区域信息（手动跟踪时必选，自动跟踪时可为 null）
     */
    public String deviceControlTargetTrack(String deviceId, String mode, String deviceId2,
                                            DeviceControlTargetTrack.TargetArea targetArea) {
        DeviceControlTargetTrack control = new DeviceControlTargetTrack(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        control.setTargetTrack(mode);
        control.setDeviceId2(deviceId2);
        control.setTargetArea(targetArea);
        return send("MESSAGE", deviceId, control);
    }

    public String deviceConfig(String deviceId, String name, String expiration,
                                String heartBeatInterval, String heartBeatCount) {
        DeviceConfigControl c = new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        c.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));
        return send("MESSAGE", deviceId, c);
    }

    /**
     * GB28181-2022 A.2.3.2.11 OSD 配置（cmdType=DeviceConfig，子标签 OSDConfig）
     */
    public String deviceConfigOsd(String deviceId, io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig.OsdInfo osdInfo) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setOsdConfig(osdInfo);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 A.2.3.2.7 报警录像配置（cmdType=DeviceConfig，子标签 VideoAlarmRecord）
     */
    public String deviceConfigVideoAlarmRecord(String deviceId,
                                                io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig.VideoAlarmRecordInfo info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setVideoAlarmRecord(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 A.2.3.2.10 报警上报开关配置（cmdType=DeviceConfig，子标签 AlarmReport）
     */
    public String deviceConfigAlarmReport(String deviceId,
                                           io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig.AlarmReportInfo info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setAlarmReport(info);
        return send("MESSAGE", deviceId, cfg);
    }

    public String deviceConfigDownload(String deviceId, String configType) {
        DeviceConfigDownload d = new DeviceConfigDownload(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), sn(), deviceId);
        d.setConfigType(configType);
        return send("MESSAGE", deviceId, d);
    }

    /**
     * GB28181-2022 §A.2.3.2.3 SVAC 编码配置（cmdType=DeviceConfig，子标签 SVACEncodeConfig）。
     */
    public String deviceConfigSvacEncode(String deviceId,
                                          io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig.SVACEncodeInfo info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setSvacEncodeConfig(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.4 SVAC 解码配置（cmdType=DeviceConfig，子标签 SVACDecodeConfig）。
     */
    public String deviceConfigSvacDecode(String deviceId,
                                          io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig.SVACDecodeInfo info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setSvacDecodeConfig(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.5 视频参数属性配置（cmdType=DeviceConfig，子标签 VideoParamAttribute）。
     */
    public String deviceConfigVideoParamAttribute(String deviceId,
                                                    io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig.VideoParamAttribute info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setVideoParamAttribute(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.2 视频参数范围配置（cmdType=DeviceConfig，子标签 VideoParamOpt，2016 遗留兼容）。
     */
    public String deviceConfigVideoParamOpt(String deviceId,
                                              io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig.VideoParamOpt info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setVideoParamOpt(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.6 录像计划配置（cmdType=DeviceConfig，子标签 VideoRecordPlan）。
     */
    public String deviceConfigVideoRecordPlan(String deviceId,
                                                io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig.VideoRecordPlan info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setVideoRecordPlan(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.8 视频遮挡区域配置（cmdType=DeviceConfig，子标签 PictureMask）。
     */
    public String deviceConfigPictureMask(String deviceId,
                                            io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig.PictureMask info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setPictureMask(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * GB28181-2022 §A.2.3.2.9 画面镜像配置（cmdType=DeviceConfig，子标签 FrameMirror）。
     */
    public String deviceConfigFrameMirror(String deviceId,
                                            io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig.FrameMirror info) {
        io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig cfg =
            new io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        cfg.setFrameMirror(info);
        return send("MESSAGE", deviceId, cfg);
    }

    /**
     * 发送广播通知（cmdType=Broadcast）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceBroadcast(String deviceId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        return send("MESSAGE", deviceId,
            new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), from.getUserId(), deviceId));
    }

    /**
     * 发送实时点播 INVITE（s=Play）。
     *
     * @param deviceId   目标设备 ID
     * @param sdpIp      收流 IP
     * @param mediaPort  收流端口
     * @param streamMode 流传输模式
     * @return SIP Call-ID
     */
    public String deviceInvitePlay(String deviceId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        ToDevice to = getToDevice(deviceId);
        to.setStreamMode(streamMode.name());
        FromDevice from = deviceSupplier.getServerFromDevice();
        InviteRequest req = new InviteRequest(deviceId, streamMode, sdpIp, mediaPort);
        return factory.getStrategy("server", "INVITE")
            .execute(CommandContext.builder()
                .role("server").commandType("INVITE")
                .fromDevice(from).toDevice(to).content(req.getContent()).build());
    }

    /**
     * 发送历史回放 INVITE（s=Playback）。
     *
     * @param deviceId   目标设备 ID
     * @param sdpIp      收流 IP
     * @param mediaPort  收流端口
     * @param streamMode 流传输模式
     * @param startTime  回放起始时间（ISO 8601）
     * @param endTime    回放结束时间（ISO 8601）
     * @return SIP Call-ID
     */
    public String deviceInvitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
                                        StreamModeEnum streamMode, String startTime, String endTime) {
        ToDevice to = getToDevice(deviceId);
        to.setStreamMode(streamMode.name());
        FromDevice from = deviceSupplier.getServerFromDevice();
        InviteRequest req = new InviteRequest(deviceId, streamMode, sdpIp, mediaPort, startTime, endTime);
        return factory.getStrategy("server", "INVITE")
            .execute(CommandContext.builder()
                .role("server").commandType("INVITE")
                .fromDevice(from).toDevice(to).content(req.getBackContent()).build());
    }

    /**
     * GB28181-2022 §9.12.2 语音对讲 INVITE (audio-only, sendonly)
     */
    public String deviceInviteTalk(String deviceId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        ToDevice to = getToDevice(deviceId);
        to.setStreamMode(streamMode.name());
        FromDevice from = deviceSupplier.getServerFromDevice();
        String ssrc = io.github.lunasaw.gb28181.common.entity.utils.GbUtil.genSsrc(deviceId);
        String content = io.github.lunasaw.gbproxy.server.entity.InviteEntity
            .getInviteTalkBody(streamMode, deviceId, sdpIp, mediaPort, ssrc).toString();
        return factory.getStrategy("server", "INVITE")
            .execute(CommandContext.builder()
                .role("server").commandType("INVITE")
                .fromDevice(from).toDevice(to).content(content).build());
    }

    /**
     * GB28181-2022 §9.9 视音频文件下载 INVITE (s=Download + a=downloadspeed)
     */
    public String deviceInviteDownload(String deviceId, String sdpIp, Integer mediaPort,
                                        StreamModeEnum streamMode, String startTime, String endTime,
                                        Integer downloadSpeed) {
        ToDevice to = getToDevice(deviceId);
        to.setStreamMode(streamMode.name());
        FromDevice from = deviceSupplier.getServerFromDevice();
        String ssrc = io.github.lunasaw.gb28181.common.entity.utils.GbUtil.genSsrc(deviceId);
        String content = io.github.lunasaw.gbproxy.server.entity.InviteEntity
            .getInviteDownloadBody(streamMode, deviceId, sdpIp, mediaPort, ssrc, startTime, endTime, downloadSpeed).toString();
        return factory.getStrategy("server", "INVITE")
            .execute(CommandContext.builder()
                .role("server").commandType("INVITE")
                .fromDevice(from).toDevice(to).content(content).build());
    }

    /**
     * 发送回放控制 INFO（暂停/恢复/定位/倍速）。
     *
     * @param deviceId        目标设备 ID
     * @param playActionEnums 回放操作类型
     * @return SIP Call-ID
     */
    public String deviceInvitePlayBackControl(String deviceId, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        return factory.getStrategy("server", "INFO")
            .execute(CommandContext.forInfo("server", from, to, controlBody));
    }

    /**
     * 发送 ACK（不带 callId，框架自动匹配事务）。
     *
     * @param deviceId 目标设备 ID
     * @return SIP Call-ID
     */
    public String deviceAck(String deviceId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        return factory.getStrategy("server", "ACK")
            .execute(CommandContext.forAckBye("server", from, to, null, "ACK"));
    }

    /**
     * 发送 ACK（指定 callId，用于 INVITE 三向握手最后一步）。
     *
     * @param deviceId 目标设备 ID
     * @param callId   INVITE 阶段的 Call-ID
     * @return SIP Call-ID
     */
    public String deviceAck(String deviceId, String callId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        to.setCallId(callId);
        return factory.getStrategy("server", "ACK")
            .execute(CommandContext.forAckBye("server", from, to, callId, "ACK"));
    }

    /**
     * 1.7.0：BYE dialog-aware 入口。不再需要 deviceId —— 信息全部从 dialog 取回。
     *
     * @param callId INVITE 阶段记录的 Call-ID
     */
    public String deviceBye(String callId) {
        return factory.getStrategy("server", "BYE")
            .execute(CommandContext.forBye("server", callId));
    }

    /**
     * 1.7.0：续订订阅。
     *
     * @param callId  初始 SUBSCRIBE 的 Call-ID
     * @param expires 续订时长（秒）
     */
    public String refreshSubscribe(String callId, int expires) {
        return SipSender.doSubscribeRefresh(callId, null, expires);
    }

    /**
     * 1.7.0：续订订阅（带 body）。
     */
    public String refreshSubscribe(String callId, String content, int expires) {
        return SipSender.doSubscribeRefresh(callId, content, expires);
    }

    /**
     * 1.7.0：退订（expires=0）。
     */
    public String unsubscribe(String callId) {
        return SipSender.doSubscribeRefresh(callId, null, 0);
    }

    /**
     * 通过 SipURI 发送 ACK（用于 INVITE 200 OK 后的 dialog-aware ACK）。
     *
     * @param from        平台发送方设备信息
     * @param sipURI      目标 SIP URI
     * @param sipResponse INVITE 200 OK 响应
     * @return SIP Call-ID
     */
    public String deviceAckBySipUri(FromDevice from, SipURI sipURI, SIPResponse sipResponse) {
        return SipSender.doAckRequest(from, sipURI, sipResponse);
    }

    // ==================== 通用发送 ====================

    /**
     * 通过 CommandContext 直接发送自定义指令。
     *
     * @param ctx 已构建好的命令上下文
     * @return SIP Call-ID
     */
    public String send(CommandContext ctx) {
        return factory.getStrategy(ctx.getRole(), ctx.getCommandType()).execute(ctx);
    }

    private String send(String commandType, String deviceId, Object body) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        return factory.getStrategy("server", commandType)
            .execute(CommandContext.builder()
                .role("server").commandType(commandType)
                .fromDevice(from).toDevice(to).body(body).build());
    }

    private ToDevice getToDevice(String deviceId) {
        ToDevice to = sessionCache.getToDevice(deviceId);
        Assert.notNull(to, "设备未注册或会话缓存中不存在: " + deviceId);
        return to;
    }

    private static String sn() {
        return RandomStrUtil.getValidationCode();
    }

}
