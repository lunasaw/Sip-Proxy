package io.github.lunasaw.gbproxy.client.transmit.cmd;

import com.luna.common.check.Assert;
import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.ClientCommandStrategy;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.ClientCommandStrategyFactory;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181客户端命令发送器
 * 使用策略模式和建造者模式，提供更灵活和可扩展的命令发送接口
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class ClientCommandSender {

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
        ClientCommandStrategy strategy;
        try {
            strategy = ClientCommandStrategyFactory.getStrategy(commandType);
        } catch (IllegalArgumentException e) {
            // 对于非SIP基础协议的命令，使用MESSAGE策略
            strategy = ClientCommandStrategyFactory.getMessageStrategy();
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
        ClientCommandStrategy strategy;
        try {
            strategy = ClientCommandStrategyFactory.getStrategy(commandType);
        } catch (IllegalArgumentException e) {
            // 对于非SIP基础协议的命令，使用MESSAGE策略
            strategy = ClientCommandStrategyFactory.getMessageStrategy();
        }
        return strategy.execute(fromDevice, toDevice, errorEvent, okEvent, params);
    }

    // ==================== 告警相关命令 ====================

    /**
     * 发送告警命令
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param deviceAlarm 告警信息
     * @return callId
     */
    public static String sendAlarmCommand(FromDevice fromDevice, ToDevice toDevice, DeviceAlarm deviceAlarm) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceAlarm);
    }

    /**
     * 发送告警命令
     *
     * @param fromDevice        发送设备
     * @param toDevice          接收设备
     * @param deviceAlarmNotify 告警通知对象
     * @return callId
     */
    public static String sendAlarmCommand(FromDevice fromDevice, ToDevice toDevice, DeviceAlarmNotify deviceAlarmNotify) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceAlarmNotify);
    }

    // ==================== 心跳相关命令 ====================

    /**
     * 发送心跳命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param status     状态信息
     * @return callId
     */
    public static String sendKeepaliveCommand(FromDevice fromDevice, ToDevice toDevice, String status) {
        DeviceKeepLiveNotify keepLiveNotify = new DeviceKeepLiveNotify(
                CmdTypeEnum.KEEPALIVE.getType(),
                RandomStrUtil.getValidationCode(),
                fromDevice.getUserId()
        );
        keepLiveNotify.setStatus(status);
        return sendKeepaliveCommand(fromDevice, toDevice, keepLiveNotify);
    }

    public static String sendKeepaliveCommand(FromDevice fromDevice, ToDevice toDevice, DeviceKeepLiveNotify deviceKeepLiveNotify) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceKeepLiveNotify);
    }

    // ==================== 设备目录相关命令 ====================

    /**
     * 发送目录命令
     *
     * @param fromDevice     发送设备
     * @param toDevice       接收设备
     * @param deviceResponse 设备响应对象
     * @return callId
     */
    public static String sendCatalogCommand(FromDevice fromDevice, ToDevice toDevice, DeviceResponse deviceResponse) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceResponse);
    }

    /**
     * 发送目录命令
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param deviceItems 设备列表
     * @return callId
     */
    public static String sendCatalogCommand(FromDevice fromDevice, ToDevice toDevice, List<DeviceItem> deviceItems) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceItems);
    }

    /**
     * 发送目录命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param deviceItem 单个设备项
     * @return callId
     */
    public static String sendCatalogCommand(FromDevice fromDevice, ToDevice toDevice, DeviceItem deviceItem) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceItem);
    }

    // ==================== 设备信息相关命令 ====================

    /**
     * 发送设备信息响应命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param deviceInfo 设备信息
     * @return callId
     */
    public static String sendDeviceInfoCommand(FromDevice fromDevice, ToDevice toDevice, DeviceInfo deviceInfo) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceInfo);
    }

    /**
     * 发送设备状态响应命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param online     在线状态 "ONLINE":"OFFLINE"
     * @return callId
     */
    public static String sendDeviceStatusCommand(FromDevice fromDevice, ToDevice toDevice, String online) {
        DeviceStatus deviceStatus = new DeviceStatus(
                CmdTypeEnum.DEVICE_STATUS.getType(),
                RandomStrUtil.getValidationCode(),
                fromDevice.getUserId()
        );
        deviceStatus.setOnline(online);
        return sendDeviceStatusCommand(fromDevice, toDevice, deviceStatus);
    }

    /**
     * 发送设备状态响应命令
     *
     * @param fromDevice   发送设备
     * @param toDevice     接收设备
     * @param deviceStatus 在线状态 "ONLINE":"OFFLINE"
     * @return callId
     */
    public static String sendDeviceStatusCommand(FromDevice fromDevice, ToDevice toDevice, DeviceStatus deviceStatus) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceStatus);
    }

    // ==================== 位置信息相关命令 ====================

    /**
     * 发送位置通知命令
     *
     * @param fromDevice           发送设备
     * @param toDevice             接收设备
     * @param mobilePositionNotify 位置通知对象
     * @param subscribeInfo        订阅信息
     * @return callId
     */
    public static String sendMobilePositionCommand(FromDevice fromDevice, ToDevice toDevice,
                                                   MobilePositionNotify mobilePositionNotify, SubscribeInfo subscribeInfo) {
        return sendCommand("MESSAGE", fromDevice, toDevice, mobilePositionNotify);
    }

    // ==================== 设备更新相关命令 ====================

    /**
     * 发送设备通道更新通知命令
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param deviceItems 通道列表
     * @return callId
     */
    public static String sendDeviceChannelUpdateCommand(FromDevice fromDevice, ToDevice toDevice,
                                                        List<DeviceUpdateItem> deviceItems) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceItems);
    }

    /**
     * 发送设备其他更新通知命令
     *
     * @param fromDevice  发送设备
     * @param toDevice    接收设备
     * @param deviceItems 推送事件列表
     * @return callId
     */
    public static String sendDeviceOtherUpdateCommand(FromDevice fromDevice, ToDevice toDevice,
                                                      List<DeviceOtherUpdateNotify.OtherItem> deviceItems) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceItems);
    }

    // ==================== 录像相关命令 ====================

    /**
     * 发送录像响应命令
     *
     * @param fromDevice   发送设备
     * @param toDevice     接收设备
     * @param deviceRecord 录像响应对象
     * @return callId
     */
    public static String sendDeviceRecordCommand(FromDevice fromDevice, ToDevice toDevice, DeviceRecord deviceRecord) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceRecord);
    }

    /**
     * 发送录像响应命令
     *
     * @param fromDevice        发送设备
     * @param toDevice          接收设备
     * @param deviceRecordItems 录像文件列表
     * @return callId
     */
    public static String sendDeviceRecordCommand(FromDevice fromDevice, ToDevice toDevice,
                                                 List<DeviceRecord.RecordItem> deviceRecordItems) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceRecordItems);
    }

    // ==================== 配置相关命令 ====================

    /**
     * 发送设备配置响应命令
     *
     * @param fromDevice           发送设备
     * @param toDevice             接收设备
     * @param deviceConfigResponse 配置响应对象
     * @return callId
     */
    public static String sendDeviceConfigCommand(FromDevice fromDevice, ToDevice toDevice, DeviceConfigResponse deviceConfigResponse) {
        return sendCommand("MESSAGE", fromDevice, toDevice, deviceConfigResponse);
    }

    // ==================== 媒体状态相关命令 ====================

    /**
     * 发送媒体状态通知命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param notifyType 通知类型 121
     * @return callId
     */
    public static String sendMediaStatusCommand(FromDevice fromDevice, ToDevice toDevice, String notifyType) {
        MediaStatusNotify mediaStatusNotify = new MediaStatusNotify(
                CmdTypeEnum.MEDIA_STATUS.getType(),
                RandomStrUtil.getValidationCode(),
                fromDevice.getUserId()
        );
        mediaStatusNotify.setNotifyType(notifyType);
        return sendCommand("MESSAGE", fromDevice, toDevice, mediaStatusNotify);
    }

    /**
     * 发送设备预置位查询应答
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param response   预置位查询应答对象
     * @return callId
     */
    public static String sendPresetQueryResponse(FromDevice fromDevice, ToDevice toDevice, PresetQueryResponse response) {
        return sendCommand("MESSAGE", fromDevice, toDevice, response);
    }


    /**
     * 发送设备预置位查询应答
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param response   预置位查询应答对象
     * @return callId
     */
    public static String sendMobilePositionNotify(FromDevice fromDevice, ToDevice toDevice, MobilePositionNotify response) {
        return sendCommand("MESSAGE", fromDevice, toDevice, response);
    }


    // ==================== 会话控制相关命令 ====================

    /**
     * 发送BYE请求命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String sendByeCommand(FromDevice fromDevice, ToDevice toDevice) {
        return sendCommand("BYE", fromDevice, toDevice);
    }

    /**
     * 发送ACK响应命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String sendAckCommand(FromDevice fromDevice, ToDevice toDevice) {
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
    public static String sendAckCommand(FromDevice fromDevice, ToDevice toDevice, String callId) {
        return sendCommand("ACK", fromDevice, toDevice, callId);
    }

    /**
     * 发送ACK响应命令（带内容和callId）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param content    内容
     * @param callId     呼叫ID
     * @return callId
     */
    public static String sendAckCommand(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return sendCommand("ACK", fromDevice, toDevice, content, callId);
    }

    // ==================== 注册相关命令 ====================

    /**
     * 发送注册命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param expires    过期时间
     * @return callId
     */
    public static String sendRegisterCommand(FromDevice fromDevice, ToDevice toDevice, Integer expires) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        return sendCommand("REGISTER", fromDevice, toDevice, expires);
    }

    /**
     * 发送注册命令（带事件）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param expires    过期时间
     * @param event      事件
     * @return callId
     */
    public static String sendRegisterCommand(FromDevice fromDevice, ToDevice toDevice, Integer expires, Event event) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        return sendCommand("REGISTER", fromDevice, toDevice, event, null, expires);
    }

    /**
     * 发送注销命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @return callId
     */
    public static String sendUnregisterCommand(FromDevice fromDevice, ToDevice toDevice) {
        return sendCommand("REGISTER", fromDevice, toDevice, 0);
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