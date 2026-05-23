package io.github.lunasaw.gbproxy.client.transmit.cmd;

import com.luna.common.check.Assert;
import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.notify.*;
import io.github.lunasaw.gb28181.common.entity.response.*;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandStrategyFactory;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ClientCommandSender implements ApplicationContextAware {

    private static ClientCommandSender INSTANCE;
    private final CommandStrategyFactory factory;

    public ClientCommandSender(CommandStrategyFactory factory) {
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
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body).build());
    }

    private static String send(String commandType, FromDevice from, ToDevice to, String content) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType(commandType)
            .fromDevice(from).toDevice(to).content(content).build());
    }

    private static String send(String commandType, FromDevice from, ToDevice to, Object body,
                                Event errorEvent, Event okEvent) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType(commandType)
            .fromDevice(from).toDevice(to).body(body)
            .errorEvent(errorEvent).okEvent(okEvent).build());
    }

    public static String sendAlarmCommand(FromDevice from, ToDevice to, DeviceAlarm alarm) {
        return send("MESSAGE", from, to, alarm);
    }

    public static String sendAlarmCommand(FromDevice from, ToDevice to, DeviceAlarmNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    public static String sendKeepaliveCommand(FromDevice from, ToDevice to, String status) {
        DeviceKeepLiveNotify n = new DeviceKeepLiveNotify(
            CmdTypeEnum.KEEPALIVE.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        n.setStatus(status);
        return send("MESSAGE", from, to, n);
    }

    public static String sendKeepaliveCommand(FromDevice from, ToDevice to, DeviceKeepLiveNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    public static String sendCatalogCommand(FromDevice from, ToDevice to, DeviceResponse response) {
        return send("MESSAGE", from, to, response);
    }

    public static String sendCatalogCommand(FromDevice from, ToDevice to, List<DeviceItem> items) {
        return send("MESSAGE", from, to, items);
    }

    public static String sendCatalogCommand(FromDevice from, ToDevice to, DeviceItem item) {
        return send("MESSAGE", from, to, item);
    }

    public static String sendDeviceInfoCommand(FromDevice from, ToDevice to, DeviceInfo info) {
        return send("MESSAGE", from, to, info);
    }

    public static String sendDeviceStatusCommand(FromDevice from, ToDevice to, String online) {
        DeviceStatus s = new DeviceStatus(
            CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        s.setOnline(online);
        return send("MESSAGE", from, to, s);
    }

    public static String sendDeviceStatusCommand(FromDevice from, ToDevice to, DeviceStatus status) {
        return send("MESSAGE", from, to, status);
    }

    public static String sendMobilePositionCommand(FromDevice from, ToDevice to, MobilePositionNotify notify, SubscribeInfo subscribeInfo) {
        return send("MESSAGE", from, to, notify);
    }

    public static String sendMobilePositionNotify(FromDevice from, ToDevice to, MobilePositionNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    public static String sendDeviceChannelUpdateCommand(FromDevice from, ToDevice to, List<DeviceUpdateItem> items) {
        return send("MESSAGE", from, to, items);
    }

    public static String sendDeviceOtherUpdateCommand(FromDevice from, ToDevice to, List<DeviceOtherUpdateNotify.OtherItem> items) {
        return send("MESSAGE", from, to, items);
    }

    public static String sendDeviceRecordCommand(FromDevice from, ToDevice to, DeviceRecord record) {
        return send("MESSAGE", from, to, record);
    }

    public static String sendDeviceRecordCommand(FromDevice from, ToDevice to, List<DeviceRecord.RecordItem> items) {
        return send("MESSAGE", from, to, items);
    }

    public static String sendDeviceConfigCommand(FromDevice from, ToDevice to, DeviceConfigResponse response) {
        return send("MESSAGE", from, to, response);
    }

    public static String sendConfigDownloadResponse(FromDevice from, ToDevice to, ConfigDownloadResponse response) {
        return send("MESSAGE", from, to, response);
    }

    public static String sendMediaStatusCommand(FromDevice from, ToDevice to, String notifyType) {
        MediaStatusNotify n = new MediaStatusNotify(
            CmdTypeEnum.MEDIA_STATUS.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        n.setNotifyType(notifyType);
        return send("MESSAGE", from, to, n);
    }

    public static String sendPresetQueryResponse(FromDevice from, ToDevice to, PresetQueryResponse response) {
        return send("MESSAGE", from, to, response);
    }

    public static String sendByeCommand(FromDevice from, ToDevice to) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, null, "BYE"));
    }

    public static String sendAckCommand(FromDevice from, ToDevice to) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, null, "ACK"));
    }

    public static String sendAckCommand(FromDevice from, ToDevice to, String callId) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, callId, "ACK"));
    }

    public static String sendAckCommand(FromDevice from, ToDevice to, String content, String callId) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, callId, "ACK")
            .toBuilder().content(content).build());
    }

    public static String sendInvitePlayCommand(FromDevice from, ToDevice to, String sdpContent) {
        return send("INVITE", from, to, sdpContent);
    }

    public static String sendInvitePlayCommand(FromDevice from, ToDevice to, String sdpContent, Event errorEvent, Event okEvent) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(sdpContent)
            .errorEvent(errorEvent).okEvent(okEvent).build());
    }

    public static String sendInvitePlayBackCommand(FromDevice from, ToDevice to, String sdpContent) {
        return send("INVITE", from, to, sdpContent);
    }

    public static String sendInvitePlayBackCommand(FromDevice from, ToDevice to, String sdpContent, Event errorEvent, Event okEvent) {
        return sendInvitePlayCommand(from, to, sdpContent, errorEvent, okEvent);
    }

    public static String sendInvitePlayControlCommand(FromDevice from, ToDevice to, String controlContent) {
        return send("MESSAGE", from, to, controlContent);
    }

    public static String sendRegisterCommand(FromDevice from, ToDevice to, Integer expires) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forRegister("client", from, to, expires));
    }

    public static String sendRegisterCommand(FromDevice from, ToDevice to, Integer expires, Event event) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forRegister("client", from, to, expires)
            .toBuilder().okEvent(event).build());
    }

    public static String sendUnregisterCommand(FromDevice from, ToDevice to) {
        return sendRegisterCommand(from, to, 0);
    }
}
