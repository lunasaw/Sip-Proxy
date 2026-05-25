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

    /**
     * GB28181-2022 §9.11.2 报警事件 NOTIFY (订阅生效后的主动上报)
     */
    public static String sendAlarmNotify(FromDevice from, ToDevice to, DeviceAlarmNotify notify) {
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

    /**
     * GB28181-2022 附录 M 多响应消息传输：把超大目录响应分批发送。
     *
     * <p>每批响应携带相同 SN（与原查询请求 SN 一致）和总数 sumNum，但只携带 batchSize 条 deviceItem。
     * 0 条时返回 sumNum=0 且不携带 deviceItemList。响应条数超过 10000 时建议改用 TCP。
     *
     * @param from           本端
     * @param to             目标平台
     * @param fullResponse   完整目录响应（携带全部 deviceItemList）
     * @param batchSize      每批最多 deviceItem 条数；建议 100~1000，最大不超过 10000
     * @return 第一批的 callId（后续批次内部串行发送，调用方无需关心）
     */
    public static String sendCatalogCommandBatched(FromDevice from, ToDevice to,
                                                    DeviceResponse fullResponse, int batchSize) {
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalArgumentException("batchSize 必须在 [1, 10000] 范围内：" + batchSize);
        }
        List<DeviceItem> all = fullResponse.getDeviceItemList();
        int total = all == null ? 0 : all.size();
        // 0 条：单条响应 sumNum=0，不携带列表
        if (total == 0) {
            DeviceResponse empty = cloneCatalogShell(fullResponse);
            empty.setSumNum(0);
            empty.setDeviceItemList(null);
            return send("MESSAGE", from, to, empty);
        }
        String firstCallId = null;
        for (int offset = 0; offset < total; offset += batchSize) {
            int end = Math.min(offset + batchSize, total);
            DeviceResponse batch = cloneCatalogShell(fullResponse);
            batch.setSumNum(total);
            batch.setDeviceItemList(all.subList(offset, end));
            String callId = send("MESSAGE", from, to, batch);
            if (firstCallId == null) {
                firstCallId = callId;
            }
        }
        return firstCallId;
    }

    private static DeviceResponse cloneCatalogShell(DeviceResponse src) {
        DeviceResponse shell = new DeviceResponse(src.getCmdType(), src.getSn(), src.getDeviceId());
        shell.setName(src.getName());
        return shell;
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

    /**
     * GB28181-2022 附录 N 域间目录订阅通知：发送目录变更通知（在线 / 离线 / 增加 / 删除 / 更新）。
     *
     * <p>对应 §N.2.1.2.1：被订阅域在检测到被订阅范围内目录变更事件时，应根据接收的订阅者列表，
     * 向处于订阅有效期的域发送目录状态通知消息。NOTIFY method 由协议层 {@code CatalogNotifyStrategy}
     * 自动选用。
     *
     * @param from   本端
     * @param to     订阅方域
     * @param sn     与订阅请求的 SN 相同（保持会话关联）
     * @param events 变更条目列表，每条 (deviceId, event)，event ∈ {ON, OFF, ADD, DEL, UPDATE}
     */
    public static String sendCatalogChangeNotify(FromDevice from, ToDevice to, String sn,
                                                  List<DeviceOtherUpdateNotify.OtherItem> events) {
        DeviceOtherUpdateNotify notify = new DeviceOtherUpdateNotify("Catalog", sn, from.getUserId());
        int total = events == null ? 0 : events.size();
        notify.setSumNum(total);
        notify.setDeviceItemList(events);
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 附录 N + 附录 M 联用：超大目录变更分批 NOTIFY。
     */
    public static String sendCatalogChangeNotifyBatched(FromDevice from, ToDevice to, String sn,
                                                        List<DeviceOtherUpdateNotify.OtherItem> events,
                                                        int batchSize) {
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalArgumentException("batchSize 必须在 [1, 10000] 范围内：" + batchSize);
        }
        int total = events == null ? 0 : events.size();
        if (total == 0) {
            DeviceOtherUpdateNotify empty = new DeviceOtherUpdateNotify("Catalog", sn, from.getUserId());
            empty.setSumNum(0);
            return send("MESSAGE", from, to, empty);
        }
        String firstCallId = null;
        for (int offset = 0; offset < total; offset += batchSize) {
            int end = Math.min(offset + batchSize, total);
            DeviceOtherUpdateNotify batch = new DeviceOtherUpdateNotify("Catalog", sn, from.getUserId());
            batch.setSumNum(total);
            batch.setDeviceItemList(events.subList(offset, end));
            String callId = send("MESSAGE", from, to, batch);
            if (firstCallId == null) {
                firstCallId = callId;
            }
        }
        return firstCallId;
    }

    public static String sendDeviceRecordCommand(FromDevice from, ToDevice to, DeviceRecord record) {
        return send("MESSAGE", from, to, record);
    }

    public static String sendDeviceRecordCommand(FromDevice from, ToDevice to, List<DeviceRecord.RecordItem> items) {
        return send("MESSAGE", from, to, items);
    }

    /**
     * GB28181-2022 附录 M 多响应消息传输：把超大录像查询响应分批发送。
     *
     * <p>每批响应携带相同 SN（与原 RecordInfo 查询请求 SN 一致）和总数 sumNum，但只携带 batchSize 条 recordItem。
     * 0 条时返回 sumNum=0 且不携带 recordList。响应条数超过 10000 时建议改用 TCP。
     */
    public static String sendDeviceRecordCommandBatched(FromDevice from, ToDevice to,
                                                        DeviceRecord fullRecord, int batchSize) {
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalArgumentException("batchSize 必须在 [1, 10000] 范围内：" + batchSize);
        }
        List<DeviceRecord.RecordItem> all = fullRecord.getRecordList();
        int total = all == null ? 0 : all.size();
        if (total == 0) {
            DeviceRecord empty = cloneRecordShell(fullRecord);
            empty.setSumNum(0);
            empty.setRecordList(null);
            return send("MESSAGE", from, to, empty);
        }
        String firstCallId = null;
        for (int offset = 0; offset < total; offset += batchSize) {
            int end = Math.min(offset + batchSize, total);
            DeviceRecord batch = cloneRecordShell(fullRecord);
            batch.setSumNum(total);
            batch.setRecordList(all.subList(offset, end));
            String callId = send("MESSAGE", from, to, batch);
            if (firstCallId == null) {
                firstCallId = callId;
            }
        }
        return firstCallId;
    }

    private static DeviceRecord cloneRecordShell(DeviceRecord src) {
        return new DeviceRecord(src.getCmdType(), src.getSn(), src.getDeviceId());
    }

    public static String sendDeviceConfigCommand(FromDevice from, ToDevice to, DeviceConfigResponse response) {
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

    /**
     * GB28181-2022 §A.2.6.9 设备配置查询应答（cmdType=ConfigDownload，携带 BasicParam/SVAC/OSD 等配置画像）。
     */
    public static String sendConfigDownloadResponse(FromDevice from, ToDevice to, DeviceConfigDownloadResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * GB28181-2022 §A.2.5.8 设备实时视音频回传通知（移动设备开始/结束实时回传时主动上报）。
     */
    public static String sendVideoUploadNotify(FromDevice from, ToDevice to, VideoUploadNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 A.2.5.9 设备软件升级结果通知
     */
    public static String sendUpgradeResultNotify(FromDevice from, ToDevice to, UpgradeResultNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 A.2.5.9 设备软件升级结果通知 (字段构造)
     */
    public static String sendUpgradeResultNotify(FromDevice from, ToDevice to,
                                                  String sessionId, String upgradeResult,
                                                  String firmware, String upgradeFailedReason) {
        UpgradeResultNotify notify = new UpgradeResultNotify(RandomStrUtil.getValidationCode(), from.getUserId());
        notify.setSessionId(sessionId);
        notify.setUpgradeResult(upgradeResult);
        notify.setFirmware(firmware);
        notify.setUpgradeFailedReason(upgradeFailedReason);
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 A.2.5.7 图像抓拍传输完成通知
     */
    public static String sendSnapShotFinishedNotify(FromDevice from, ToDevice to, UploadSnapShotFinishedNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 A.2.5.7 图像抓拍传输完成通知 (字段构造)
     */
    public static String sendSnapShotFinishedNotify(FromDevice from, ToDevice to,
                                                     String sessionId, List<String> snapShotFileIds) {
        UploadSnapShotFinishedNotify notify = new UploadSnapShotFinishedNotify(RandomStrUtil.getValidationCode(), from.getUserId());
        notify.setSessionId(sessionId);
        notify.setSnapShotFileIds(snapShotFileIds);
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 A.2.6.15 PTZ 精确状态查询应答
     */
    public static String sendPtzPositionResponse(FromDevice from, ToDevice to, PTZPositionResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * GB28181-2022 A.2.6.16 存储卡状态查询应答
     */
    public static String sendSdCardStatusResponse(FromDevice from, ToDevice to, SDCardStatusResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * GB28181-2022 A.2.6.12 看守位信息查询应答
     */
    public static String sendHomePositionResponse(FromDevice from, ToDevice to, HomePositionResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * GB28181-2022 A.2.6.13 巡航轨迹列表查询应答
     */
    public static String sendCruiseTrackListResponse(FromDevice from, ToDevice to, CruiseTrackListResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * GB28181-2022 A.2.6.14 巡航轨迹查询应答
     */
    public static String sendCruiseTrackResponse(FromDevice from, ToDevice to, CruiseTrackResponse response) {
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
