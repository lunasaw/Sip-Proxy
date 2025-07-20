package io.github.lunasaw.gbproxy.server.transimit.cmd;

import java.util.Date;
import java.util.Optional;

import javax.sip.address.SipURI;

import org.springframework.util.Assert;

import com.luna.common.date.DateUtils;
import com.luna.common.text.RandomStrUtil;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gb28181.common.entity.control.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.query.*;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gb28181.common.entity.utils.PtzUtils;
import io.github.lunasaw.gbproxy.server.entity.InviteRequest;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.SipSender;

/**
 * @author luna
 * @date 2023/10/14
 */
public class ServerSendCmd {

    /**
     * 设备信息查询
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String deviceInfo(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceQuery.toString());
    }

    /**
     * 设备预设位置查询
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String devicePresetQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceQuery.toString());
    }

    /**
     * 查询移动设备位置数据
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String devicePresetQuery(FromDevice fromDevice, ToDevice toDevice, String interval) {
        DeviceMobileQuery deviceMobileQuery =
            new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceMobileQuery.setInterval(interval);

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceMobileQuery.toString());
    }

    /**
     * 订阅移动设备位置数据
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String devicePresetSubscribe(FromDevice fromDevice, ToDevice toDevice, String interval, Integer expires, String eventType,
        String eventId) {
        DeviceMobileQuery deviceMobileQuery =
            new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceMobileQuery.setInterval(interval);

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);

        return SipSender.doSubscribeRequest(fromDevice, toDevice, deviceMobileQuery.toString(), subscribeInfo);
    }

    /**
     * 设备状态查询
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String deviceStatusQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceQuery.toString());
    }

    /**
     * 设备通道列表查询
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String deviceCatalogQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceQuery.toString());
    }

    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, String startTime, String endTime) {
        return deviceRecordInfoQuery(fromDevice, toDevice, startTime, endTime, null, "all");
    }

    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, long startTime, long endTime) {
        return deviceRecordInfoQuery(fromDevice, toDevice, new Date(startTime), new Date(endTime));
    }

    /**
     * 查询录像列表
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param startTime 开始时间 ISO8601格式
     * @param endTime 结束时间 ISO8601格式
     * @param secrecy 保密属性 缺省为0; 0:不涉密, 1:涉密
     * @param type all（time 或 alarm 或 manual 或 all）
     * @return
     */
    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, String startTime, String endTime, String secrecy,
        String type) {
        DeviceRecordQuery recordQuery =
            new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());

        recordQuery.setStartTime(startTime);
        recordQuery.setEndTime(endTime);
        recordQuery.setSecrecy(secrecy);
        recordQuery.setType(type);

        return SipSender.doMessageRequest(fromDevice, toDevice, recordQuery.toString());
    }

    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, Date startTime, Date endTime) {
        return deviceRecordInfoQuery(fromDevice, toDevice, startTime, endTime, "0", "all");
    }

    /**
     * 查询录像列表
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return callId
     */
    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, Date startTime, Date endTime, String secrecy, String type) {

        String startTimeStr = DateUtils.formatTime(DateUtils.ISO8601_PATTERN, startTime);
        String endTimeStr = DateUtils.formatTime(DateUtils.ISO8601_PATTERN, endTime);

        return deviceRecordInfoQuery(fromDevice, toDevice, startTimeStr, endTimeStr, secrecy, type);
    }

    public static String deviceCatalogSubscribe(FromDevice fromDevice, ToDevice toDevice,
        Integer expires, String eventType) {
        return deviceCatalogSubscribe(fromDevice, toDevice, expires, eventType, RandomStrUtil.generateNonceStr());
    }

    /**
     * 会话订阅
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param expires 过期时间
     * @param eventType 事件类型
     * @return
     */
    public static String deviceCatalogSubscribe(FromDevice fromDevice, ToDevice toDevice,
        Integer expires, String eventType, String eventId) {
        DeviceQuery recordQuery = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);

        return SipSender.doSubscribeRequest(fromDevice, toDevice, recordQuery.toString(), subscribeInfo);
    }

    /**
     * 告警查询
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param startPriority 报警起始级别（可选）
     * @param endPriority 报警终止级别（可选）
     * @param alarmMethod 报警方式条件（可选）
     * @param alarmType 报警类型
     * @param startTime 报警发生起始时间（可选）
     * @param endTime 报警发生终止时间（可选）
     * @return callId
     */
    public static String deviceAlarmQuery(FromDevice fromDevice, ToDevice toDevice, Date startTime, Date endTime, String startPriority,
        String endPriority, String alarmMethod, String alarmType) {
        DeviceAlarmQuery deviceAlarmQuery =
            new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());

        deviceAlarmQuery.setStartTime(DateUtils.formatTime(DateUtils.ISO8601_PATTERN, startTime));
        deviceAlarmQuery.setEndTime(DateUtils.formatTime(DateUtils.ISO8601_PATTERN, endTime));
        deviceAlarmQuery.setStartAlarmPriority(startPriority);
        deviceAlarmQuery.setEndAlarmPriority(endPriority);
        deviceAlarmQuery.setAlarmType(alarmType);

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceAlarmQuery.toString());
    }

    /**
     * 回复ACK
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return
     */
    public static String deviceAck(FromDevice fromDevice, ToDevice toDevice) {
        return SipSender.doAckRequest(fromDevice, toDevice);
    }

    public static String deviceAck(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return SipSender.doAckRequest(fromDevice, toDevice, callId);
    }

    public static String deviceAck(FromDevice fromDevice, SipURI sipURI, SIPResponse sipResponse) {
        return SipSender.doAckRequest(fromDevice, sipURI, sipResponse);
    }

    /**
     * 发送BYE
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return
     */
    public static String deviceBye(FromDevice fromDevice, ToDevice toDevice) {
        return SipSender.doByeRequest(fromDevice, toDevice);
    }

    /**
     * 设备广播
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return
     */
    public static String deviceBroadcast(FromDevice fromDevice, ToDevice toDevice) {
        DeviceBroadcastNotify deviceBroadcastNotify =
            new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId(),
                toDevice.getUserId());

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceBroadcastNotify.toString());
    }

    /**
     * 报警布防/撤防命令
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param guardCmdStr "SetGuard"/"ResetGuard"
     * @return
     */
    public static String deviceControlGuardCmd(FromDevice fromDevice, ToDevice toDevice, String guardCmdStr) {
        DeviceControlGuard deviceControl =
            new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        deviceControl.setGuardCmd(guardCmdStr);
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControl.toString());
    }

    /**
     * 报警复位命令
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param alarmMethod 方式
     * @param alarmType 类型
     * @return
     */
    public static String deviceControlAlarm(FromDevice fromDevice, ToDevice toDevice, String alarmMethod, String alarmType) {

        DeviceControlAlarm deviceControlAlarm = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(),
            fromDevice.getUserId());

        deviceControlAlarm.setAlarmCmd("ResetAlarm");
        deviceControlAlarm.setAlarmInfo(new DeviceControlAlarm.AlarmInfo(alarmMethod, alarmType));

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlAlarm.toString());
    }

    public static String deviceControlAlarm(FromDevice fromDevice, ToDevice toDevice, DeviceControlPosition.HomePosition homePosition) {
        DeviceControlPosition deviceControlPosition =
            new DeviceControlPosition(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlPosition.toString());
    }

    /**
     * 看守位控制命令
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param enable 看守位使能：1 = 开启，0 = 关闭
     * @param resetTime 自动归位时间间隔，开启看守位时使用，单位:秒(s)
     * @param presetIndex 调用预置位编号，开启看守位时使用，取值范围0~255
     * @return
     */
    public static String deviceControlAlarm(FromDevice fromDevice, ToDevice toDevice, String enable, String resetTime, String presetIndex) {

        DeviceControlPosition.HomePosition homePosition = new DeviceControlPosition.HomePosition(enable, resetTime, presetIndex);

        return deviceControlAlarm(fromDevice, toDevice, homePosition);
    }

    /**
     * 设备配置命令：basicParam
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param name 设备/通道名称（可选）
     * @param expiration 注册过期时间（可选）
     * @param heartBeatInterval 心跳间隔时间（可选）
     * @param heartBeatCount 心跳超时次数（可选）
     * @return
     */
    public static String deviceConfig(FromDevice fromDevice, ToDevice toDevice, String name, String expiration,
        String heartBeatInterval, String heartBeatCount) {

        DeviceConfigControl deviceConfigControl =
            new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), RandomStrUtil.getValidationCode(),
                fromDevice.getUserId());

        deviceConfigControl.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceConfigControl.toString());
    }

    /**
     * 下载设备配置
     *
     * @param fromDevice
     * @param toDevice
     * @param configType 配置类型
     * @return
     */
    public static String deviceConfigDownload(FromDevice fromDevice, ToDevice toDevice, String configType) {

        DeviceConfigDownload deviceConfig =
            new DeviceConfigDownload(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), RandomStrUtil.getValidationCode(),
                fromDevice.getUserId());

        deviceConfig.setConfigType(configType);

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceConfig.toString());
    }

    /**
     * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return
     */
    public static String deviceControlIdr(FromDevice fromDevice, ToDevice toDevice, String cmdStr) {
        DeviceControlIFame deviceControlIFame =
            new DeviceControlIFame(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        String cmd = Optional.ofNullable(cmdStr).orElse("Send");
        deviceControlIFame.setIFameCmd(cmd);
        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlIFame.toString());
    }

    /**
     * 伸缩控制
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param dragZoom 缩小
     * @return
     */
    public static String deviceControlDragOut(FromDevice fromDevice, ToDevice toDevice, DragZoom dragZoom) {
        DeviceControlDragOut dragZoomOut =
            new DeviceControlDragOut(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        dragZoomOut.setDragZoomOut(dragZoom);

        return SipSender.doMessageRequest(fromDevice, toDevice, dragZoomOut.toString());
    }

    /**
     * 伸缩控制
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param dragZoom 放大
     * @return
     */
    public static String deviceControlDragIn(FromDevice fromDevice, ToDevice toDevice, DragZoom dragZoom) {
        DeviceControlDragIn dragZoomIn =
            new DeviceControlDragIn(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        dragZoomIn.setDragZoomIn(dragZoom);

        return SipSender.doMessageRequest(fromDevice, toDevice, dragZoomIn.toString());
    }

    /**
     * 云台控制命令
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param ptzCmdEnum 命令
     * @param speed 速度
     * @return
     */
    public static String deviceControlPtzCmd(FromDevice fromDevice, ToDevice toDevice, PtzCmdEnum ptzCmdEnum, Integer speed) {
        String ptzCmd = PtzUtils.getPtzCmd(ptzCmdEnum, speed);
        return deviceControlPtzCmd(fromDevice, toDevice, ptzCmd);
    }

    /**
     * 设备命令控制
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param ptzCmd 控制代码
     * @return
     */
    public static String deviceControlPtzCmd(FromDevice fromDevice, ToDevice toDevice, String ptzCmd) {
        DeviceControlPtz deviceControlPtz =
            new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        deviceControlPtz.setPtzCmd(ptzCmd);
        deviceControlPtz.setPtzInfo(new DeviceControlPtz.PtzInfo());

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlPtz.toString());
    }

    /**
     * 设备重启
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @return
     */
    public static String deviceControlTeleBoot(FromDevice fromDevice, ToDevice toDevice) {
        DeviceControlTeleBoot deviceControlTeleBoot =
            new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlTeleBoot.toString());
    }

    /**
     * 录像控制
     *
     * @param fromDevice 发送设备
     * @param toDevice 接收设备
     * @param recordCmd Record 开启录像， StopRecord 停止录像
     * @return
     */
    public static String deviceControlTeleBoot(FromDevice fromDevice, ToDevice toDevice, String recordCmd) {
        DeviceControlRecordCmd deviceControlRecordCmd =
            new DeviceControlRecordCmd(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());

        deviceControlRecordCmd.setRecordCmd(recordCmd);

        return SipSender.doMessageRequest(fromDevice, toDevice, deviceControlRecordCmd.toString());
    }

    /**
     * 设备实时流点播
     *
     * @param fromDevice
     * @param toDevice
     * @param sdpIp
     * @param mediaPort
     * @return
     */
    public static String deviceInvitePlay(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort) {
        InviteRequest inviteRequest = new InviteRequest(toDevice.getUserId(), StreamModeEnum.valueOf(toDevice.getStreamMode()), sdpIp, mediaPort);
        return deviceInvitePlay(fromDevice, toDevice, inviteRequest);
    }

    /**
     * 设备实时流点播
     *
     * @param fromDevice
     * @param toDevice
     * @param inviteRequest
     * @return
     */
    public static String deviceInvitePlay(FromDevice fromDevice, ToDevice toDevice, InviteRequest inviteRequest) {

        String subject = inviteRequest.getSubject(fromDevice.getUserId());

        String content = inviteRequest.getContent();

        return SipSender.doInviteRequest(fromDevice, toDevice, content, subject);
    }

    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort, String startTime) {

        Date startDate = DateUtils.parseDateTime(startTime);

        return deviceInvitePlayBack(fromDevice, toDevice, sdpIp, mediaPort, startDate, DateUtils.getNight(startDate));
    }

    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort, Date startDate) {

        return deviceInvitePlayBack(fromDevice, toDevice, sdpIp, mediaPort, startDate, DateUtils.getNight(startDate));
    }

    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort, Date startDate,
        Date endDate) {

        Assert.notNull(startDate, "startDate is null");
        Assert.notNull(endDate, "endDate is null");

        String startTime = DateUtils.formatDateTime(startDate);
        String endTime = DateUtils.formatDateTime(endDate);

        InviteRequest inviteRequest =
            new InviteRequest(toDevice.getUserId(), StreamModeEnum.valueOf(toDevice.getStreamMode()), sdpIp, mediaPort, startTime,
                endTime);
        return deviceInvitePlayBack(fromDevice, toDevice, inviteRequest);
    }

    /**
     * 设备回放流点播
     *
     * @param fromDevice
     * @param toDevice
     * @param sdpIp
     * @param mediaPort
     * @return
     */
    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort, String startTime,
        String endTime) {
        StreamModeEnum streamModeEnum = StreamModeEnum.valueOf(toDevice.getStreamMode());
        InviteRequest inviteRequest = new InviteRequest(toDevice.getUserId(), streamModeEnum, sdpIp, mediaPort, startTime, endTime);
        return deviceInvitePlayBack(fromDevice, toDevice, inviteRequest);
    }

    /**
     * 设备回放流点播
     *
     * @param fromDevice
     * @param toDevice
     * @param inviteRequest
     * @return
     */
    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, InviteRequest inviteRequest) {

        String subject = inviteRequest.getSubject(fromDevice.getUserId());

        String content = inviteRequest.getBackContent();

        return SipSender.doInviteRequest(fromDevice, toDevice, content, subject);
    }

    /**
     * 设备回放流点播控制
     *
     * @param fromDevice
     * @param toDevice
     * @param playActionEnums 操作类型
     * @return
     */
    public static String deviceInvitePlayBackControl(FromDevice fromDevice, ToDevice toDevice, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        return SipSender.doInfoRequest(fromDevice, toDevice, controlBody);
    }

    /**
     * 通用设备控制命令发送（支持所有DeviceControlBase子类）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param controlCmd 控制命令对象（如DeviceControlPtz、DeviceControlGuard等）
     * @return callId
     */
    public static String deviceControl(FromDevice fromDevice, ToDevice toDevice, DeviceControlBase controlCmd) {
        Assert.notNull(controlCmd, "控制命令对象不能为空");
        return SipSender.doMessageRequest(fromDevice, toDevice, controlCmd.toString());
    }
}
