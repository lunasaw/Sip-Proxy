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

    public String deviceConfig(String deviceId, String name, String expiration,
                                String heartBeatInterval, String heartBeatCount) {
        DeviceConfigControl c = new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), sn(), deviceId);
        c.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));
        return send("MESSAGE", deviceId, c);
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
