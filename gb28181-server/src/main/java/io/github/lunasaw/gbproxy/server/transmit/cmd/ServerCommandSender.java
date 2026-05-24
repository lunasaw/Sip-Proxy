package io.github.lunasaw.gbproxy.server.transmit.cmd;

import com.luna.common.check.Assert;
import com.luna.common.date.DateUtils;
import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gb28181.common.entity.utils.PtzUtils;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandStrategyFactory;
import io.github.lunasaw.gbproxy.server.entity.InviteRequest;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.address.SipURI;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerCommandSender {

    private final CommandStrategyFactory factory;
    private final ServerDeviceSupplier deviceSupplier;
    private final DeviceSessionCache sessionCache;

    // ==================== 实例方法（新 API） ====================

    public String deviceInfoQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), sn(), deviceId));
    }

    public String deviceStatusQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), sn(), deviceId));
    }

    public String deviceCatalogQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), deviceId));
    }

    public String devicePresetQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), sn(), deviceId));
    }

    public String deviceRecordInfoQuery(String deviceId, String startTime, String endTime) {
        DeviceRecordQuery q = new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), sn(), deviceId);
        q.setStartTime(startTime);
        q.setEndTime(endTime);
        return send("MESSAGE", deviceId, q);
    }

    public String deviceRecordInfoQuery(String deviceId, long startTime, long endTime) {
        return deviceRecordInfoQuery(deviceId,
            DateUtils.formatDateTime(new Date(startTime)),
            DateUtils.formatDateTime(new Date(endTime)));
    }

    public String deviceMobilePositionQuery(String deviceId, String interval) {
        DeviceMobileQuery q = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), sn(), deviceId);
        q.setInterval(interval);
        return send("MESSAGE", deviceId, q);
    }

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

    public String deviceControlGuardCmd(String deviceId, String guardCmdStr) {
        DeviceControlGuard g = new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        g.setGuardCmd(guardCmdStr);
        return send("MESSAGE", deviceId, g);
    }

    public String deviceControlAlarm(String deviceId, String alarmMethod, String alarmType) {
        DeviceControlAlarm a = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        DeviceControlAlarm.AlarmInfo info = new DeviceControlAlarm.AlarmInfo();
        info.setAlarmMethod(alarmMethod);
        info.setAlarmType(alarmType);
        a.setAlarmInfo(info);
        return send("MESSAGE", deviceId, a);
    }

    public String deviceControlPtzCmd(String deviceId, PtzCmdEnum ptzCmdEnum, Integer speed) {
        return deviceControlPtzCmd(deviceId, PtzUtils.getPtzCmd(ptzCmdEnum, speed));
    }

    public String deviceControlPtzCmd(String deviceId, String ptzCmd) {
        DeviceControlPtz p = new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId);
        p.setPtzCmd(ptzCmd);
        p.setPtzInfo(new DeviceControlPtz.PtzInfo());
        return send("MESSAGE", deviceId, p);
    }

    public String deviceControlReboot(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), deviceId));
    }

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

    public String deviceBroadcast(String deviceId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        return send("MESSAGE", deviceId,
            new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), from.getUserId(), deviceId));
    }

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

    public String deviceInvitePlayBackControl(String deviceId, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        return factory.getStrategy("server", "INFO")
            .execute(CommandContext.forInfo("server", from, to, controlBody));
    }

    public String deviceAck(String deviceId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        return factory.getStrategy("server", "ACK")
            .execute(CommandContext.forAckBye("server", from, to, null, "ACK"));
    }

    public String deviceAck(String deviceId, String callId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        to.setCallId(callId);
        return factory.getStrategy("server", "ACK")
            .execute(CommandContext.forAckBye("server", from, to, callId, "ACK"));
    }

    public String deviceBye(String deviceId, String callId) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = getToDevice(deviceId);
        to.setCallId(callId);
        return factory.getStrategy("server", "BYE")
            .execute(CommandContext.forAckBye("server", from, to, callId, "BYE"));
    }

    public String deviceAckBySipUri(FromDevice from, SipURI sipURI, SIPResponse sipResponse) {
        return SipSender.doAckRequest(from, sipURI, sipResponse);
    }

    // ==================== 通用发送 ====================

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

    // ==================== @Deprecated 旧静态 API（过渡期保留） ====================

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInfoQuery(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), sn(), to.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceStatusQuery(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), sn(), to.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceCatalogQuery(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), to.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String devicePresetQuery(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), sn(), to.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, String startTime, String endTime) {
        DeviceRecordQuery q = new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), sn(), to.getUserId());
        q.setStartTime(startTime);
        q.setEndTime(endTime);
        return getInstance().send("MESSAGE", from, to, q);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, long startTime, long endTime) {
        return deviceRecordInfoQuery(from, to,
            DateUtils.formatDateTime(new Date(startTime)),
            DateUtils.formatDateTime(new Date(endTime)));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, Date startTime, Date endTime) {
        return deviceRecordInfoQuery(from, to,
            DateUtils.formatDateTime(startTime),
            DateUtils.formatDateTime(endTime));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceMobilePositionQuery(FromDevice from, ToDevice to, String interval) {
        DeviceMobileQuery q = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), sn(), to.getUserId());
        q.setInterval(interval);
        return getInstance().send("MESSAGE", from, to, q);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceCatalogSubscribe(FromDevice from, ToDevice to, Integer expires, String eventType) {
        DeviceQuery body = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), to.getUserId());
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return getInstance().send(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
            .toBuilder().body(body).build());
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceMobilePositionSubscribe(FromDevice from, ToDevice to, String interval,
                                                        Integer expires, String eventType, String eventId) {
        DeviceMobileQuery body = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), sn(), to.getUserId());
        body.setInterval(interval);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return getInstance().send(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
            .toBuilder().body(body).build());
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceAlarmQuery(FromDevice from, ToDevice to, Date startTime, Date endTime,
                                           String startPriority, String endPriority,
                                           String alarmMethod, String alarmType) {
        DeviceAlarmQuery q = new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), sn(), to.getUserId());
        q.setStartTime(DateUtils.formatDateTime(startTime));
        q.setEndTime(DateUtils.formatDateTime(endTime));
        q.setStartAlarmPriority(startPriority);
        q.setEndAlarmPriority(endPriority);
        q.setAlarmMethod(alarmMethod);
        q.setAlarmType(alarmType);
        return getInstance().send("MESSAGE", from, to, q);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlGuardCmd(FromDevice from, ToDevice to, String guardCmdStr) {
        DeviceControlGuard g = new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), from.getUserId());
        g.setGuardCmd(guardCmdStr);
        return getInstance().send("MESSAGE", from, to, g);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlAlarm(FromDevice from, ToDevice to, String alarmMethod, String alarmType) {
        DeviceControlAlarm a = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), from.getUserId());
        DeviceControlAlarm.AlarmInfo info = new DeviceControlAlarm.AlarmInfo();
        info.setAlarmMethod(alarmMethod);
        info.setAlarmType(alarmType);
        a.setAlarmInfo(info);
        return getInstance().send("MESSAGE", from, to, a);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlPtzCmd(FromDevice from, ToDevice to, PtzCmdEnum ptzCmdEnum, Integer speed) {
        return deviceControlPtzCmd(from, to, PtzUtils.getPtzCmd(ptzCmdEnum, speed));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlPtzCmd(FromDevice from, ToDevice to, String ptzCmd) {
        DeviceControlPtz p = new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), from.getUserId());
        p.setPtzCmd(ptzCmd);
        p.setPtzInfo(new DeviceControlPtz.PtzInfo());
        return getInstance().send("MESSAGE", from, to, p);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlReboot(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), from.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceControlRecord(FromDevice from, ToDevice to, String recordCmd) {
        DeviceControlRecordCmd r = new DeviceControlRecordCmd(CmdTypeEnum.DEVICE_CONTROL.getType(), sn(), from.getUserId());
        r.setRecordCmd(recordCmd);
        return getInstance().send("MESSAGE", from, to, r);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceConfig(FromDevice from, ToDevice to, String name, String expiration,
                                       String heartBeatInterval, String heartBeatCount) {
        DeviceConfigControl c = new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), from.getUserId());
        c.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));
        return getInstance().send("MESSAGE", from, to, c);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceConfigDownload(FromDevice from, ToDevice to, String configType) {
        DeviceConfigDownload d = new DeviceConfigDownload(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), sn(), from.getUserId());
        d.setConfigType(configType);
        return getInstance().send("MESSAGE", from, to, d);
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceBroadcast(FromDevice from, ToDevice to) {
        return getInstance().send("MESSAGE", from, to,
            new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), from.getUserId(), to.getUserId()));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInvitePlay(FromDevice from, ToDevice to, String sdpIp, Integer mediaPort) {
        return deviceInvitePlay(from, to,
            new InviteRequest(to.getUserId(), StreamModeEnum.valueOf(to.getStreamMode()), sdpIp, mediaPort));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInvitePlay(FromDevice from, ToDevice to, InviteRequest inviteRequest) {
        return getInstance().send(CommandContext.builder()
            .role("server").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(inviteRequest.getContent()).build());
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInvitePlayBack(FromDevice from, ToDevice to, String sdpIp, Integer mediaPort,
                                               String startTime, String endTime) {
        return deviceInvitePlayBack(from, to,
            new InviteRequest(to.getUserId(), StreamModeEnum.valueOf(to.getStreamMode()), sdpIp, mediaPort, startTime, endTime));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInvitePlayBack(FromDevice from, ToDevice to, InviteRequest inviteRequest) {
        return getInstance().send(CommandContext.builder()
            .role("server").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(inviteRequest.getBackContent()).build());
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceInvitePlayBackControl(FromDevice from, ToDevice to, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        return getInstance().send(CommandContext.forInfo("server", from, to, controlBody));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceAck(FromDevice from, ToDevice to) {
        return getInstance().send(CommandContext.forAckBye("server", from, to, null, "ACK"));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceAck(FromDevice from, ToDevice to, String callId) {
        return getInstance().send(CommandContext.forAckBye("server", from, to, callId, "ACK"));
    }

    /** @deprecated 请注入 {@link ServerCommandSender} 并使用实例方法 */
    @Deprecated
    public static String deviceBye(FromDevice from, ToDevice to) {
        return getInstance().send(CommandContext.forAckBye("server", from, to, null, "BYE"));
    }

    /** @deprecated 请使用 {@link #deviceAckBySipUri(FromDevice, SipURI, SIPResponse)} */
    @Deprecated
    public static String deviceAck(FromDevice from, SipURI sipURI, SIPResponse sipResponse) {
        return SipSender.doAckRequest(from, sipURI, sipResponse);
    }

    // ==================== 旧静态方法内部辅助 ====================

    private static String send(String commandType, FromDevice from, ToDevice to, Object body) {
        return getInstance().send(CommandContext.builder()
            .role("server").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body).build());
    }

    private static String send(String commandType, FromDevice from, ToDevice to, Object body,
                                Event errorEvent, Event okEvent) {
        return getInstance().send(CommandContext.builder()
            .role("server").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body)
            .errorEvent(errorEvent).okEvent(okEvent).build());
    }

    // 静态方法委托给 Spring Bean 实例
    private static ServerCommandSender INSTANCE;

    @jakarta.annotation.PostConstruct
    void registerInstance() {
        INSTANCE = this;
    }

    private static ServerCommandSender getInstance() {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE;
    }
}
