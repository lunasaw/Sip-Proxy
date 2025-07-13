package io.github.lunasaw.gbproxy.server.transimit.cmd;

import com.luna.common.check.Assert;
import com.luna.common.date.DateUtils;
import com.luna.common.text.RandomStrUtil;
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
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.ServerCommandStrategy;
import io.github.lunasaw.gbproxy.server.transimit.cmd.strategy.ServerCommandStrategyFactory;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * GB28181服务端命令发送器
 * 使用策略模式和建造者模式，提供更灵活和可扩展的命令发送接口
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ServerCommandSender {

    // ==================== 策略模式命令发送 ====================

    /**
     * 使用策略模式发送命令
     *
     * @param commandType 命令类型
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param params      命令参数
     * @return callId
     */
    public static String sendCommand(String commandType, FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // 如果没有对应的策略，使用MESSAGE策略
        ServerCommandStrategy strategy;
        try {
            strategy = ServerCommandStrategyFactory.getStrategy(commandType);
        } catch (IllegalArgumentException e) {
            // 对于非SIP基础协议的命令，使用MESSAGE策略
            strategy = ServerCommandStrategyFactory.getMessageStrategy();
        }
        return strategy.execute(fromDevice, toDevice, params);
    }

    /**
     * 使用策略模式发送命令（带事件）
     *
     * @param commandType 命令类型
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param errorEvent  错误事件
     * @param okEvent     成功事件
     * @param params      命令参数
     * @return callId
     */
    public static String sendCommand(String commandType, FromDevice fromDevice, ToDevice toDevice, Event errorEvent, Event okEvent,
                                     Object... params) {
        // 如果没有对应的策略，使用MESSAGE策略
        ServerCommandStrategy strategy;
        try {
            strategy = ServerCommandStrategyFactory.getStrategy(commandType);
        } catch (IllegalArgumentException e) {
            // 对于非SIP基础协议的命令，使用MESSAGE策略
            strategy = ServerCommandStrategyFactory.getMessageStrategy();
        }
        return strategy.execute(fromDevice, toDevice, errorEvent, okEvent, params);
    }

    // ==================== 设备信息查询相关命令 ====================

    /**
     * 设备信息查询
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceInfoQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceQuery);
    }

    /**
     * 设备状态查询
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceStatusQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceQuery);
    }

    /**
     * 设备目录查询
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceCatalogQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceQuery);
    }

    /**
     * 设备预设位置查询
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String devicePresetQuery(FromDevice fromDevice, ToDevice toDevice) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.PRESET_QUERY.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceQuery);
    }

    // ==================== 设备录像查询相关命令 ====================

    /**
     * 设备录像信息查询
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return callId
     */
    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, String startTime, String endTime) {
        DeviceRecordQuery deviceRecordQuery = new DeviceRecordQuery(CmdTypeEnum.RECORD_INFO.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceRecordQuery.setStartTime(startTime);
        deviceRecordQuery.setEndTime(endTime);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceRecordQuery);
    }

    /**
     * 设备录像信息查询（时间戳）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param startTime  开始时间戳
     * @param endTime    结束时间戳
     * @return callId
     */
    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, long startTime, long endTime) {
        String startTimeStr = DateUtils.formatDateTime(new Date(startTime));
        String endTimeStr = DateUtils.formatDateTime(new Date(endTime));
        return deviceRecordInfoQuery(fromDevice, toDevice, startTimeStr, endTimeStr);
    }

    /**
     * 设备录像信息查询（Date对象）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return callId
     */
    public static String deviceRecordInfoQuery(FromDevice fromDevice, ToDevice toDevice, Date startTime, Date endTime) {
        String startTimeStr = DateUtils.formatDateTime(startTime);
        String endTimeStr = DateUtils.formatDateTime(endTime);
        return deviceRecordInfoQuery(fromDevice, toDevice, startTimeStr, endTimeStr);
    }

    // ==================== 设备移动位置相关命令 ====================

    /**
     * 查询移动设备位置数据
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param interval   间隔
     * @return callId
     */
    public static String deviceMobilePositionQuery(FromDevice fromDevice, ToDevice toDevice, String interval) {
        DeviceMobileQuery deviceMobileQuery = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceMobileQuery.setInterval(interval);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceMobileQuery);
    }

    // ==================== 设备订阅相关命令 ====================

    /**
     * 设备目录订阅
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param expires    过期时间
     * @param eventType  事件类型
     * @return callId
     */
    public static String deviceCatalogSubscribe(FromDevice fromDevice, ToDevice toDevice, Integer expires, String eventType) {
        DeviceQuery deviceQuery = new DeviceQuery(CmdTypeEnum.CATALOG.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);

        return sendCommand("SUBSCRIBE", fromDevice, toDevice, deviceQuery, subscribeInfo);
    }

    /**
     * 订阅移动设备位置数据
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param interval   间隔
     * @param expires    过期时间
     * @param eventType  事件类型
     * @param eventId    事件ID
     * @return callId
     */
    public static String deviceMobilePositionSubscribe(FromDevice fromDevice, ToDevice toDevice, String interval, Integer expires, String eventType, String eventId) {
        DeviceMobileQuery deviceMobileQuery = new DeviceMobileQuery(CmdTypeEnum.MOBILE_POSITION.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceMobileQuery.setInterval(interval);

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventId(eventId);
        subscribeInfo.setEventType(eventType);
        subscribeInfo.setExpires(expires);

        return sendCommand("SUBSCRIBE", fromDevice, toDevice, deviceMobileQuery, subscribeInfo);
    }

    // ==================== 设备告警相关命令 ====================

    /**
     * 设备告警查询
     *
     * @param fromDevice    发送设备
     * @param toDevice      接收设备
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param startPriority 开始优先级
     * @param endPriority   结束优先级
     * @param alarmMethod   告警方式
     * @param alarmType     告警类型
     * @return callId
     */
    public static String deviceAlarmQuery(FromDevice fromDevice, ToDevice toDevice, Date startTime, Date endTime,
                                          String startPriority, String endPriority, String alarmMethod, String alarmType) {
        DeviceAlarmQuery deviceAlarmQuery = new DeviceAlarmQuery(CmdTypeEnum.ALARM.getType(), RandomStrUtil.getValidationCode(), toDevice.getUserId());
        deviceAlarmQuery.setStartTime(DateUtils.formatDateTime(startTime));
        deviceAlarmQuery.setEndTime(DateUtils.formatDateTime(endTime));
        deviceAlarmQuery.setStartAlarmPriority(startPriority);
        deviceAlarmQuery.setEndAlarmPriority(endPriority);
        deviceAlarmQuery.setAlarmMethod(alarmMethod);
        deviceAlarmQuery.setAlarmType(alarmType);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceAlarmQuery);
    }

    // ==================== 设备控制相关命令 ====================

    /**
     * 设备守卫控制
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param guardCmdStr 守卫命令字符串
     * @return callId
     */
    public static String deviceControlGuardCmd(FromDevice fromDevice, ToDevice toDevice, String guardCmdStr) {
        DeviceControlGuard deviceControlGuard = new DeviceControlGuard(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        deviceControlGuard.setGuardCmd(guardCmdStr);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceControlGuard);
    }

    /**
     * 设备告警控制
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param alarmMethod 告警方式
     * @param alarmType   告警类型
     * @return callId
     */
    public static String deviceControlAlarm(FromDevice fromDevice, ToDevice toDevice, String alarmMethod, String alarmType) {
        DeviceControlAlarm deviceControlAlarm = new DeviceControlAlarm(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        DeviceControlAlarm.AlarmInfo alarmInfo = new DeviceControlAlarm.AlarmInfo();
        alarmInfo.setAlarmMethod(alarmMethod);
        alarmInfo.setAlarmType(alarmType);
        deviceControlAlarm.setAlarmInfo(alarmInfo);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceControlAlarm);
    }

    /**
     * 设备云台控制
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param ptzCmdEnum 云台命令
     * @param speed      速度
     * @return callId
     */
    public static String deviceControlPtzCmd(FromDevice fromDevice, ToDevice toDevice, PtzCmdEnum ptzCmdEnum, Integer speed) {
        String ptzCmd = PtzUtils.getPtzCmd(ptzCmdEnum, speed);
        return deviceControlPtzCmd(fromDevice, toDevice, ptzCmd);
    }

    /**
     * 设备云台控制
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param ptzCmd     云台命令
     * @return callId
     */
    public static String deviceControlPtzCmd(FromDevice fromDevice, ToDevice toDevice, String ptzCmd) {
        DeviceControlPtz deviceControlPtz = new DeviceControlPtz(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        deviceControlPtz.setPtzCmd(ptzCmd);
        deviceControlPtz.setPtzInfo(new DeviceControlPtz.PtzInfo());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceControlPtz);
    }

    /**
     * 设备重启控制
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceControlReboot(FromDevice fromDevice, ToDevice toDevice) {
        DeviceControlTeleBoot deviceControlTeleBoot = new DeviceControlTeleBoot(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceControlTeleBoot);
    }

    /**
     * 设备录像控制
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param recordCmd  录像命令 Record/StopRecord
     * @return callId
     */
    public static String deviceControlRecord(FromDevice fromDevice, ToDevice toDevice, String recordCmd) {
        DeviceControlRecordCmd deviceControlRecordCmd = new DeviceControlRecordCmd(CmdTypeEnum.DEVICE_CONTROL.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        deviceControlRecordCmd.setRecordCmd(recordCmd);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceControlRecordCmd);
    }

    // ==================== 设备配置相关命令 ====================

    /**
     * 设备配置
     *
     * @param fromDevice        发送设备
     * @param toDevice          接收设备
     * @param name              设备名称
     * @param expiration        过期时间
     * @param heartBeatInterval 心跳间隔
     * @param heartBeatCount    心跳次数
     * @return callId
     */
    public static String deviceConfig(FromDevice fromDevice, ToDevice toDevice, String name, String expiration,
                                      String heartBeatInterval, String heartBeatCount) {

        DeviceConfigControl deviceConfigControl =
                new DeviceConfigControl(CmdTypeEnum.DEVICE_CONFIG.getType(), RandomStrUtil.getValidationCode(),
                        fromDevice.getUserId());

        deviceConfigControl.setBasicParam(new DeviceConfigControl.BasicParam(name, expiration, heartBeatInterval, heartBeatCount));

        return sendCommand("MESSAGE", fromDevice, toDevice, deviceConfigControl);
    }

    /**
     * 设备配置下载
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param configType 配置类型
     * @return callId
     */
    public static String deviceConfigDownload(FromDevice fromDevice, ToDevice toDevice, String configType) {
        DeviceConfigDownload deviceConfigDownload = new DeviceConfigDownload(CmdTypeEnum.CONFIG_DOWNLOAD.getType(), RandomStrUtil.getValidationCode(), fromDevice.getUserId());
        deviceConfigDownload.setConfigType(configType);
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceConfigDownload);
    }

    // ==================== 设备广播相关命令 ====================

    /**
     * 设备广播
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceBroadcast(FromDevice fromDevice, ToDevice toDevice) {
        DeviceBroadcastNotify deviceBroadcastNotify = new DeviceBroadcastNotify(CmdTypeEnum.BROADCAST.getType(), fromDevice.getUserId(), toDevice.getUserId());
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceBroadcastNotify);
    }

    // ==================== 设备点播相关命令 ====================

    /**
     * 设备实时流点播
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param sdpIp      SDP IP
     * @param mediaPort  媒体端口
     * @return callId
     */
    public static String deviceInvitePlay(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort) {
        InviteRequest inviteRequest = new InviteRequest(toDevice.getUserId(), StreamModeEnum.valueOf(toDevice.getStreamMode()), sdpIp, mediaPort);
        return deviceInvitePlay(fromDevice, toDevice, inviteRequest);
    }

    /**
     * 设备实时流点播
     *
     * @param fromDevice    发送设备
     * @param toDevice      接收设备
     * @param inviteRequest 邀请请求
     * @return callId
     */
    public static String deviceInvitePlay(FromDevice fromDevice, ToDevice toDevice, InviteRequest inviteRequest) {
        String content = inviteRequest.getContent();
        return sendCommand("INVITE", fromDevice, toDevice, content);
    }

    /**
     * 设备回放流点播
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param sdpIp      SDP IP
     * @param mediaPort  媒体端口
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return callId
     */
    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, String sdpIp, Integer mediaPort,
                                              String startTime, String endTime) {
        StreamModeEnum streamModeEnum = StreamModeEnum.valueOf(toDevice.getStreamMode());
        InviteRequest inviteRequest = new InviteRequest(toDevice.getUserId(), streamModeEnum, sdpIp, mediaPort, startTime, endTime);
        return deviceInvitePlayBack(fromDevice, toDevice, inviteRequest);
    }

    /**
     * 设备回放流点播
     *
     * @param fromDevice    发送设备
     * @param toDevice      接收设备
     * @param inviteRequest 邀请请求
     * @return callId
     */
    public static String deviceInvitePlayBack(FromDevice fromDevice, ToDevice toDevice, InviteRequest inviteRequest) {
        String content = inviteRequest.getBackContent();
        return sendCommand("INVITE", fromDevice, toDevice, content);
    }

    /**
     * 设备回放流点播控制
     *
     * @param fromDevice      发送设备
     * @param toDevice        接收设备
     * @param playActionEnums 播放操作
     * @return callId
     */
    public static String deviceInvitePlayBackControl(FromDevice fromDevice, ToDevice toDevice, PlayActionEnums playActionEnums) {
        String controlBody = playActionEnums.getControlBody();
        Assert.notNull(controlBody, "不支持的操作类型");
        return sendCommand("INFO", fromDevice, toDevice, controlBody);
    }

    // ==================== 会话控制相关命令 ====================

    /**
     * 发送ACK响应命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceAck(FromDevice fromDevice, ToDevice toDevice) {
        return sendCommand("ACK", fromDevice, toDevice);
    }

    /**
     * 发送ACK响应命令（指定callId）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param callId     呼叫ID
     * @return callId
     */
    public static String deviceAck(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return sendCommand("ACK", fromDevice, toDevice, callId);
    }

    /**
     * 发送BYE请求命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String deviceBye(FromDevice fromDevice, ToDevice toDevice) {
        return sendCommand("BYE", fromDevice, toDevice);
    }

    // ==================== 建造者模式 ====================

    /**
     * 命令发送建造者
     * 提供流式API，支持链式调用
     */
    public static class CommandBuilder {
        private String commandType;
        private FromDevice fromDevice;
        private ToDevice toDevice;
        private Event errorEvent;
        private Event okEvent;
        private SubscribeInfo subscribeInfo;
        private Object[] params;

        public CommandBuilder commandType(String commandType) {
            this.commandType = commandType;
            return this;
        }

        public CommandBuilder fromDevice(FromDevice fromDevice) {
            this.fromDevice = fromDevice;
            return this;
        }

        public CommandBuilder toDevice(ToDevice toDevice) {
            this.toDevice = toDevice;
            return this;
        }

        public CommandBuilder errorEvent(Event errorEvent) {
            this.errorEvent = errorEvent;
            return this;
        }

        public CommandBuilder okEvent(Event okEvent) {
            this.okEvent = okEvent;
            return this;
        }

        public CommandBuilder subscribeInfo(SubscribeInfo subscribeInfo) {
            this.subscribeInfo = subscribeInfo;
            return this;
        }

        public CommandBuilder params(Object... params) {
            this.params = params;
            return this;
        }

        public String execute() {
            if (subscribeInfo != null) {
                return sendCommand(commandType, fromDevice, toDevice, subscribeInfo, params);
            } else {
                return sendCommand(commandType, fromDevice, toDevice, errorEvent, okEvent, params);
            }
        }
    }

    /**
     * 创建命令建造者
     *
     * @return 命令建造者
     */
    public static CommandBuilder builder() {
        return new CommandBuilder();
    }
}