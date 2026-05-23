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
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.sip.address.SipURI;
import java.util.Date;
import java.util.HashMap;

@Slf4j
@Component
public class ServerCommandSender implements ApplicationContextAware {

    private static ServerCommandSender INSTANCE;
    private final CommandStrategyFactory factory;

    public ServerCommandSender(CommandStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        INSTANCE = this;
    }

    public String send(CommandContext ctx) {
        return factory.getStrategy(ctx.getRole(), ctx.getCommandType()).execute(ctx);
    }

    private static String send(String commandType, FromDevice from, ToDevice to, Object body) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("server").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body).build());
    }

    private static String send(String commandType, FromDevice from, ToDevice to, Object body,
                                Event errorEvent, Event okEvent) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("server").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body)
            .errorEvent(errorEvent).okEvent(okEvent).build());
    }

    public static String deviceInfoQuery(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), RandomStrUtil.getValidationCode(), to.getUserId()));
    }

    public static String deviceStatusQuery(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), to.getUserId()));
    }

    public static String deviceCatalogQuery(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), to.getUserId()));
    }

    public static String devicePresetQuery(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), RandomStrUtil.getValidationCode(), to.getUserId()));
    }

    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, String startTime, String endTime) {
        DeviceRecordQuery q = new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), RandomStrUtil.getValidationCode(), to.getUserId());
        q.setStartTime(startTime);
        q.setEndTime(endTime);
        return send("MESSAGE", from, to, q);
    }

    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, long startTime, long endTime) {
        return deviceRecordInfoQuery(from, to, DateUtils.formatDateTime(new Date(startTime)), DateUtils.formatDateTime(new Date(endTime)));
    }

    public static String deviceRecordInfoQuery(FromDevice from, ToDevice to, Date startTime, Date endTime) {
        return deviceRecordInfoQuery(from, to, DateUtils.formatDateTime(startTime), DateUtils.formatDateTime(endTime));
    }

    public static String deviceMobilePositionQuery(FromDevice from, ToDevice to, String interval) {
        DeviceMobileQuery q = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), to.getUserId());
        q.setInterval(interval);
        return send("MESSAGE", from, to, q);
    }

    public static String deviceCatalogSubscribe(FromDevice from, ToDevice to, Integer expires, String eventType) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        DeviceQuery body = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), to.getUserId());
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return INSTANCE.send(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
            .toBuilder().body(body).build());
    }

    public static String deviceMobilePositionSubscribe(FromDevice from, ToDevice to, String interval,
                                                        Integer expires, String eventType, String eventId) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        DeviceMobileQuery body = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), to.getUserId());
        body.setInterval(interval);
        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);
        return INSTANCE.send(CommandContext.forSubscribe("server", from, to, subscribeInfo, expires)
            .toBuilder().body(body).build());
    }

    public static String deviceAlarmQuery(FromDevice from, ToDevice to, Date startTime, Date endTime,
                                           String startPriority, String endPriority, String alarmMethod, String alarmType) {
        DeviceAlarmQuery q = new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), RandomStrUtil.getValidationCode(), to.getUserId());
        q.setStartTime(DateUtils.formatDateTime(startTime));
        q.setEndTime(DateUtils.formatDateTime(endTime));
        q.setStartAlarmPriority(startPriority);
        q.setEndAlarmPriority(endPriority);
        q.setAlarmMethod(alarmMethod);
        q.setAlarmType(alarmType);
        return send("MESSAGE", from, to, q);
    }

    public static String deviceControlGuardCmd(FromDevice from, ToDevice to, String guardCmdStr) {
        DeviceControlGuard g = new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        g.setGuardCmd(guardCmdStr);
        return send("MESSAGE", from, to, g);
    }

    public static String deviceControlAlarm(FromDevice from, ToDevice to, String alarmMethod, String alarmType) {
        DeviceControlAlarm a = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        DeviceControlAlarm.AlarmInfo info = new DeviceControlAlarm.AlarmInfo();
        info.setAlarmMethod(alarmMethod);
        info.setAlarmType(alarmType);
        a.setAlarmInfo(info);
        return send("MESSAGE", from, to, a);
    }

    public static String deviceControlPtzCmd(FromDevice from, ToDevice to, PtzCmdEnum ptzCmdEnum, Integer speed) {
        return deviceControlPtzCmd(from, to, PtzUtils.getPtzCmd(ptzCmdEnum, speed));
    }

    public static String deviceControlPtzCmd(FromDevice from, ToDevice to, String ptzCmd) {
        DeviceControlPtz p = new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        p.setPtzCmd(ptzCmd);
        p.setPtzInfo(new DeviceControlPtz.PtzInfo());
        return send("MESSAGE", from, to, p);
    }

    public static String deviceControlReboot(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), from.getUserId()));
    }

    public static String deviceControlRecord(FromDevice from, ToDevice to, String recordCmd) {
        DeviceControlRecordCmd r = new DeviceControlRecordCmd(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        r.setRecordCmd(recordCmd);
        return send("MESSAGE", from, to, r);
    }

    public static String deviceConfig(FromDevice from, ToDevice to, String name, String expiration,
                                       String heartBeatInterval, String heartBeatCount) {
        DeviceConfigControl c = new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        c.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));
        return send("MESSAGE", from, to, c);
    }

    public static String deviceConfigDownload(FromDevice from, ToDevice to, String configType) {
        DeviceConfigDownload d = new DeviceConfigDownload(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        d.setConfigType(configType);
        return send("MESSAGE", from, to, d);
    }

    public static String deviceConfigDownloadQuery(FromDevice from, ToDevice to, String configType) {
        ConfigDownloadQuery q = new ConfigDownloadQuery();
        q.setSn(RandomStrUtil.getValidationCode());
        q.setDeviceId(to.getUserId());
        q.setConfigType(configType);
        return send("MESSAGE", from, to, q);
    }

    public static String deviceBroadcast(FromDevice from, ToDevice to) {
        return send("MESSAGE", from, to,
            new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), from.getUserId(), to.getUserId()));
    }

    public static String deviceInvitePlay(FromDevice from, ToDevice to, String sdpIp, Integer mediaPort) {
        return deviceInvitePlay(from, to, new InviteRequest(to.getUserId(), StreamModeEnum.valueOf(to.getStreamMode()), sdpIp, mediaPort));
    }

    public static String deviceInvitePlay(FromDevice from, ToDevice to, InviteRequest inviteRequest) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("server").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(inviteRequest.getContent()).build());
    }

    public static String deviceInvitePlayBack(FromDevice from, ToDevice to, String sdpIp, Integer mediaPort,
                                               String startTime, String endTime) {
        return deviceInvitePlayBack(from, to,
            new InviteRequest(to.getUserId(), StreamModeEnum.valueOf(to.getStreamMode()), sdpIp, mediaPort, startTime, endTime));
    }

    public static String deviceInvitePlayBack(FromDevice from, ToDevice to, InviteRequest inviteRequest) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("server").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(inviteRequest.getBackContent()).build());
    }

    public static String deviceInvitePlayBackControl(FromDevice from, ToDevice to, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        return INSTANCE.send(CommandContext.forInfo("server", from, to, controlBody));
    }

    public static String deviceAck(FromDevice from, ToDevice to) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("server", from, to, null, "ACK"));
    }

    public static String deviceAck(FromDevice from, ToDevice to, String callId) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("server", from, to, callId, "ACK"));
    }

    public static String deviceBye(FromDevice from, ToDevice to) {
        Assert.notNull(INSTANCE, "ServerCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("server", from, to, null, "BYE"));
    }

    public static String deviceAck(FromDevice from, SipURI sipURI, SIPResponse sipResponse) {
        return SipSender.doAckRequest(from, sipURI, sipResponse);
    }
}
