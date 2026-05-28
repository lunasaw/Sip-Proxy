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
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端（设备侧）出向 SIP 命令发送器，封装所有 GB28181 client 角色的出向消息。
 *
 * <p>所有 {@code sendXxx} 方法均为静态工具方法，内部通过单例 {@code INSTANCE} 委托给
 * {@link CommandStrategyFactory} 选取对应策略执行。
 *
 * <p>1.7.0 起，BYE / SUBSCRIBE 刷新 / 退订均为 dialog-aware，必须先建立 dialog 才能调用。
 */
@Slf4j
@Component
public class ClientCommandSender implements ApplicationContextAware {

    /** 单例引用，由 Spring 容器初始化后注入。 */
    private static ClientCommandSender INSTANCE;
    /** 命令策略工厂，根据 role + commandType 选取具体发送策略。 */
    private final CommandStrategyFactory factory;

    /**
     * 构造方法，由 Spring 注入命令策略工厂。
     *
     * @param factory 命令策略工厂
     */
    public ClientCommandSender(CommandStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        INSTANCE = this;
    }

    /**
     * 执行命令上下文中指定的 SIP 命令。
     *
     * @param ctx 命令上下文，包含 role、commandType、fromDevice、toDevice 及消息体
     * @return 本次请求的 Call-ID
     */
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

    /**
     * 发送设备告警命令（MESSAGE，body=DeviceAlarm）。
     *
     * @param from  本端设备
     * @param to    目标平台
     * @param alarm 告警信息
     * @return 本次请求的 Call-ID
     */
    public static String sendAlarmCommand(FromDevice from, ToDevice to, DeviceAlarm alarm) {
        return send("MESSAGE", from, to, alarm);
    }

    /**
     * 发送设备告警通知命令（MESSAGE，body=DeviceAlarmNotify）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param notify 告警通知
     * @return 本次请求的 Call-ID
     */
    public static String sendAlarmCommand(FromDevice from, ToDevice to, DeviceAlarmNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * GB28181-2022 §9.11.2 报警事件 NOTIFY (订阅生效后的主动上报)
     */
    public static String sendAlarmNotify(FromDevice from, ToDevice to, DeviceAlarmNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * 发送心跳命令（MESSAGE，body=DeviceKeepLiveNotify，自动生成 SN）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param status 设备在线状态
     * @return 本次请求的 Call-ID
     */
    public static String sendKeepaliveCommand(FromDevice from, ToDevice to, String status) {
        DeviceKeepLiveNotify n = new DeviceKeepLiveNotify(
            CmdTypeEnum.KEEPALIVE.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        n.setStatus(status);
        return send("MESSAGE", from, to, n);
    }

    /**
     * 发送心跳命令（MESSAGE，body=DeviceKeepLiveNotify，直接传入通知对象）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param notify 心跳通知对象
     * @return 本次请求的 Call-ID
     */
    public static String sendKeepaliveCommand(FromDevice from, ToDevice to, DeviceKeepLiveNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * 发送目录响应命令（MESSAGE，body=DeviceResponse）。
     *
     * @param from     本端设备
     * @param to       目标平台
     * @param response 目录响应对象
     * @return 本次请求的 Call-ID
     */
    public static String sendCatalogCommand(FromDevice from, ToDevice to, DeviceResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * 发送目录响应命令（MESSAGE，body=List&lt;DeviceItem&gt;）。
     *
     * @param from  本端设备
     * @param to    目标平台
     * @param items 目录条目列表
     * @return 本次请求的 Call-ID
     */
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

    /**
     * 发送单条目录条目命令（MESSAGE，body=DeviceItem）。
     *
     * @param from 本端设备
     * @param to   目标平台
     * @param item 单条目录条目
     * @return 本次请求的 Call-ID
     */
    public static String sendCatalogCommand(FromDevice from, ToDevice to, DeviceItem item) {
        return send("MESSAGE", from, to, item);
    }

    /**
     * 发送设备信息查询应答命令（MESSAGE，body=DeviceInfo）。
     *
     * @param from 本端设备
     * @param to   目标平台
     * @param info 设备信息响应
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceInfoCommand(FromDevice from, ToDevice to, DeviceInfo info) {
        return send("MESSAGE", from, to, info);
    }

    /**
     * 发送设备状态查询应答命令（MESSAGE，自动构造 DeviceStatus，body=online 字段）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param online 在线状态字符串
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceStatusCommand(FromDevice from, ToDevice to, String online) {
        DeviceStatus s = new DeviceStatus(
            CmdTypeEnum.DEVICE_STATUS.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        s.setOnline(online);
        return send("MESSAGE", from, to, s);
    }

    /**
     * 发送设备状态查询应答命令（MESSAGE，body=DeviceStatus）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param status 设备状态响应对象
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceStatusCommand(FromDevice from, ToDevice to, DeviceStatus status) {
        return send("MESSAGE", from, to, status);
    }

    /**
     * 发送移动位置通知命令（MESSAGE，body=MobilePositionNotify，含订阅信息）。
     *
     * @param from          本端设备
     * @param to            目标平台
     * @param notify        移动位置通知
     * @param subscribeInfo 关联的订阅信息
     * @return 本次请求的 Call-ID
     */
    public static String sendMobilePositionCommand(FromDevice from, ToDevice to, MobilePositionNotify notify, SubscribeInfo subscribeInfo) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * 发送移动位置通知命令（MESSAGE，body=MobilePositionNotify）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param notify 移动位置通知
     * @return 本次请求的 Call-ID
     */
    public static String sendMobilePositionNotify(FromDevice from, ToDevice to, MobilePositionNotify notify) {
        return send("MESSAGE", from, to, notify);
    }

    /**
     * 发送设备通道更新命令（MESSAGE，body=List&lt;DeviceUpdateItem&gt;）。
     *
     * @param from  本端设备
     * @param to    目标平台
     * @param items 通道更新条目列表
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceChannelUpdateCommand(FromDevice from, ToDevice to, List<DeviceUpdateItem> items) {
        return send("MESSAGE", from, to, items);
    }

    /**
     * 发送其他设备更新通知命令（MESSAGE，body=List&lt;OtherItem&gt;）。
     *
     * @param from  本端设备
     * @param to    目标平台
     * @param items 其他更新条目列表
     * @return 本次请求的 Call-ID
     */
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

    /**
     * 发送录像查询应答命令（MESSAGE，body=DeviceRecord）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param record 录像查询响应对象
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceRecordCommand(FromDevice from, ToDevice to, DeviceRecord record) {
        return send("MESSAGE", from, to, record);
    }

    /**
     * 发送录像查询应答命令（MESSAGE，body=List&lt;RecordItem&gt;）。
     *
     * @param from  本端设备
     * @param to    目标平台
     * @param items 录像条目列表
     * @return 本次请求的 Call-ID
     */
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

    /**
     * 发送设备配置查询应答命令（MESSAGE，body=DeviceConfigResponse）。
     *
     * @param from     本端设备
     * @param to       目标平台
     * @param response 设备配置响应对象
     * @return 本次请求的 Call-ID
     */
    public static String sendDeviceConfigCommand(FromDevice from, ToDevice to, DeviceConfigResponse response) {
        return send("MESSAGE", from, to, response);
    }

    /**
     * 发送媒体状态通知命令（MESSAGE，自动构造 MediaStatusNotify）。
     *
     * @param from       本端设备
     * @param to         目标平台
     * @param notifyType 通知类型
     * @return 本次请求的 Call-ID
     */
    public static String sendMediaStatusCommand(FromDevice from, ToDevice to, String notifyType) {
        MediaStatusNotify n = new MediaStatusNotify(
            CmdTypeEnum.MEDIA_STATUS.getType(), RandomStrUtil.getValidationCode(), from.getUserId());
        n.setNotifyType(notifyType);
        return send("MESSAGE", from, to, n);
    }

    /**
     * 发送预置位查询应答命令（MESSAGE，body=PresetQueryResponse）。
     *
     * @param from     本端设备
     * @param to       目标平台
     * @param response 预置位查询响应对象
     * @return 本次请求的 Call-ID
     */
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

    /**
     * 1.7.0：BYE dialog-aware 入口。client 主动 BYE 同样要求 dialog 已建立。
     *
     * @param callId 初始 INVITE（如对讲）的 Call-ID
     */
    public static String sendByeCommand(String callId) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forBye("client", callId));
    }

    /**
     * 1.7.0：续订订阅（不带 body）。
     *
     * @param callId  初始 SUBSCRIBE 的 Call-ID
     * @param expires 续订有效期（秒）
     * @return 本次请求的 Call-ID
     */
    public static String refreshSubscribe(String callId, int expires) {
        return SipSender.doSubscribeRefresh(callId, null, expires);
    }

    /**
     * 1.7.0：续订订阅（带 body）。
     *
     * @param callId  初始 SUBSCRIBE 的 Call-ID
     * @param content 订阅消息体内容
     * @param expires 续订有效期（秒）
     * @return 本次请求的 Call-ID
     */
    public static String refreshSubscribe(String callId, String content, int expires) {
        return SipSender.doSubscribeRefresh(callId, content, expires);
    }

    /**
     * 1.7.0：退订（expires=0）。
     *
     * @param callId 初始 SUBSCRIBE 的 Call-ID
     * @return 本次请求的 Call-ID
     */
    public static String unsubscribe(String callId) {
        return SipSender.doSubscribeRefresh(callId, null, 0);
    }

    /**
     * 发送 ACK 命令（不带 callId）。
     *
     * @param from 本端设备
     * @param to   目标平台
     * @return 本次请求的 Call-ID
     */
    public static String sendAckCommand(FromDevice from, ToDevice to) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, null, "ACK"));
    }

    /**
     * 发送 ACK 命令（指定 callId）。
     *
     * @param from   本端设备
     * @param to     目标平台
     * @param callId 关联的 Call-ID
     * @return 本次请求的 Call-ID
     */
    public static String sendAckCommand(FromDevice from, ToDevice to, String callId) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, callId, "ACK"));
    }

    /**
     * 发送 ACK 命令（指定 content 和 callId）。
     *
     * @param from    本端设备
     * @param to      目标平台
     * @param content ACK 消息体内容
     * @param callId  关联的 Call-ID
     * @return 本次请求的 Call-ID
     */
    public static String sendAckCommand(FromDevice from, ToDevice to, String content, String callId) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forAckBye("client", from, to, callId, "ACK")
            .toBuilder().content(content).build());
    }

    /**
     * 发送实时视频点播 INVITE 命令（SDP 内容）。
     *
     * @param from       本端设备
     * @param to         目标平台
     * @param sdpContent SDP 消息体
     * @return 本次请求的 Call-ID
     */
    public static String sendInvitePlayCommand(FromDevice from, ToDevice to, String sdpContent) {
        return send("INVITE", from, to, sdpContent);
    }

    /**
     * 发送实时视频点播 INVITE 命令（带回调事件）。
     *
     * @param from       本端设备
     * @param to         目标平台
     * @param sdpContent SDP 消息体
     * @param errorEvent 失败回调事件
     * @param okEvent    成功回调事件
     * @return 本次请求的 Call-ID
     */
    public static String sendInvitePlayCommand(FromDevice from, ToDevice to, String sdpContent, Event errorEvent, Event okEvent) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType("INVITE")
            .fromDevice(from).toDevice(to).content(sdpContent)
            .errorEvent(errorEvent).okEvent(okEvent).build());
    }

    /**
     * 发送历史视频回放 INVITE 命令（SDP 内容）。
     *
     * @param from       本端设备
     * @param to         目标平台
     * @param sdpContent SDP 消息体
     * @return 本次请求的 Call-ID
     */
    public static String sendInvitePlayBackCommand(FromDevice from, ToDevice to, String sdpContent) {
        return send("INVITE", from, to, sdpContent);
    }

    /**
     * 发送历史视频回放 INVITE 命令（带回调事件）。
     *
     * @param from       本端设备
     * @param to         目标平台
     * @param sdpContent SDP 消息体
     * @param errorEvent 失败回调事件
     * @param okEvent    成功回调事件
     * @return 本次请求的 Call-ID
     */
    public static String sendInvitePlayBackCommand(FromDevice from, ToDevice to, String sdpContent, Event errorEvent, Event okEvent) {
        return sendInvitePlayCommand(from, to, sdpContent, errorEvent, okEvent);
    }

    /**
     * 发送回放控制命令（MESSAGE，body=controlContent）。
     *
     * @param from           本端设备
     * @param to             目标平台
     * @param controlContent 回放控制内容
     * @return 本次请求的 Call-ID
     */
    public static String sendInvitePlayControlCommand(FromDevice from, ToDevice to, String controlContent) {
        return send("MESSAGE", from, to, controlContent);
    }

    /**
     * 发送注册命令（REGISTER，指定有效期）。
     *
     * @param from    本端设备
     * @param to      目标平台
     * @param expires 注册有效期（秒），必须 &gt;= 0
     * @return 本次请求的 Call-ID
     */
    public static String sendRegisterCommand(FromDevice from, ToDevice to, Integer expires) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forRegister("client", from, to, expires));
    }

    /**
     * 发送注册命令（REGISTER，指定有效期，带成功回调事件）。
     *
     * @param from    本端设备
     * @param to      目标平台
     * @param expires 注册有效期（秒），必须 &gt;= 0
     * @param event   注册成功回调事件
     * @return 本次请求的 Call-ID
     */
    public static String sendRegisterCommand(FromDevice from, ToDevice to, Integer expires, Event event) {
        Assert.isTrue(expires >= 0, "过期时间应该 >= 0");
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.forRegister("client", from, to, expires)
            .toBuilder().okEvent(event).build());
    }

    /**
     * 发送注销命令（REGISTER，expires=0）。
     *
     * @param from 本端设备
     * @param to   目标平台
     * @return 本次请求的 Call-ID
     */
    public static String sendUnregisterCommand(FromDevice from, ToDevice to) {
        return sendRegisterCommand(from, to, 0);
    }
}
