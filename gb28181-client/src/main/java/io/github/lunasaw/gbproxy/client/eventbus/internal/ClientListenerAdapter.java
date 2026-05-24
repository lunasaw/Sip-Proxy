package io.github.lunasaw.gbproxy.client.eventbus.internal;

import io.github.lunasaw.gb28181.common.entity.control.DeviceConfigControl;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlAlarm;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragIn;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlDragOut;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlGuard;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlIFame;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPTZPrecise;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPosition;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlRecordCmd;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlSDCardFormat;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTargetTrack;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTeleBoot;
import io.github.lunasaw.gb28181.common.entity.control.DeviceUpgradeControl;
import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.FrameMirrorConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.PictureMaskConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACDecodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.SVACEncodeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamAttributeConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoParamOptConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoRecordPlanConfig;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackListQuery;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.DeviceMobileQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.query.HomePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PTZPositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.query.SDCardStatusQuery;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PresetQueryResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import io.github.lunasaw.gbproxy.client.api.ConfigListener;
import io.github.lunasaw.gbproxy.client.api.ControlListener;
import io.github.lunasaw.gbproxy.client.api.NotifyListener;
import io.github.lunasaw.gbproxy.client.api.QueryListener;
import io.github.lunasaw.gbproxy.client.api.SubscribeListener;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientConfigEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientControlEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientKeepaliveEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientNotifyEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientQueryEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientSubscribeEvent;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Layer 2 内部分发器：监听 6 类 L1 协议事件，按 payload 类型分发到业务方实现的 listener。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>Query 单 listener 强制</b>：{@code ObjectProvider#getIfUnique()} —— 多实例 fail fast；
 *       0 个实例首次告警一次后静默走默认空响应</li>
 *   <li><b>Control / Config / Subscribe / Notify 多 listener 全调用</b>：观察者模式，业务/metrics/audit
 *       可同时监听</li>
 *   <li><b>Config 用 Class&lt;?&gt; → Consumer 显式映射</b>：当前 5 个 config 类是 DeviceControlBase 的
 *       兄弟节点，instanceof 顺序不敏感，但映射表能在未来父子化重构时免改</li>
 *   <li><b>无状态单例</b>：{@code @Component} 注册一次，整个进程共用</li>
 * </ul>
 *
 * @author luna
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClientListenerAdapter {

    /** Query 必须唯一，多实例 fail fast。 */
    private final ObjectProvider<QueryListener> queryListenerProvider;

    /** Control / Config / Subscribe / Notify 允许多实例，全部调用。 */
    private final ObjectProvider<List<ControlListener>> controlListeners;
    private final ObjectProvider<List<ConfigListener>> configListeners;
    private final ObjectProvider<List<SubscribeListener>> subscribeListeners;
    private final ObjectProvider<List<NotifyListener>> notifyListeners;

    private final ClientDeviceSupplier supplier;

    /** 业务方未注册 QueryListener 时的首次告警闸门，避免日志洪水。 */
    private final AtomicBoolean queryListenerMissingWarned = new AtomicBoolean(false);

    // ============ Query 分发 ============

    @EventListener
    public void dispatch(ClientQueryEvent event) {
        QueryListener listener = queryListenerProvider.getIfUnique();
        if (listener == null) {
            warnQueryListenerMissingOnce();
            return;
        }

        XmlBean q = event.getQuery();

        // 分支一：DeviceQuery 共用 cmdType=Catalog/DeviceInfo/DeviceStatus，按字符串区分
        if (q instanceof DeviceQuery dq) {
            handleDeviceQuery(listener, event, dq);
            return;
        }

        // 分支二：其余 query 按 Java 类型分发（Java 17 instanceof pattern）
        FromDevice from = supplier.getClientFromDevice();
        if (from == null) {
            log.warn("ClientDeviceSupplier.getClientFromDevice() 返回 null，无法回包: query={}",
                    q.getClass().getSimpleName());
            return;
        }
        ToDevice to = requireToDevice(event.getSipId());
        if (to == null) {
            return;
        }

        if (q instanceof DeviceRecordQuery rq) {
            DeviceRecord resp = listener.onRecordInfoQuery(event.getSipId(), rq);
            if (resp != null) {
                ClientCommandSender.sendDeviceRecordCommand(from, to, resp);
            }
        } else if (q instanceof DeviceAlarmQuery aq) {
            DeviceAlarmNotify resp = listener.onAlarmQuery(event.getSipId(), aq);
            if (resp != null) {
                ClientCommandSender.sendAlarmCommand(from, to, resp);
            }
        } else if (q instanceof PTZPositionQuery pq) {
            PTZPositionResponse resp = listener.onPtzPositionQuery(event.getSipId(), pq);
            if (resp != null) {
                ClientCommandSender.sendPtzPositionResponse(from, to, resp);
            }
        } else if (q instanceof SDCardStatusQuery sq) {
            SDCardStatusResponse resp = listener.onSdCardStatusQuery(event.getSipId(), sq);
            if (resp != null) {
                ClientCommandSender.sendSdCardStatusResponse(from, to, resp);
            }
        } else if (q instanceof HomePositionQuery hq) {
            HomePositionResponse resp = listener.onHomePositionQuery(event.getSipId(), hq);
            if (resp != null) {
                ClientCommandSender.sendHomePositionResponse(from, to, resp);
            }
        } else if (q instanceof CruiseTrackListQuery clq) {
            CruiseTrackListResponse resp = listener.onCruiseTrackListQuery(event.getSipId(), clq);
            if (resp != null) {
                ClientCommandSender.sendCruiseTrackListResponse(from, to, resp);
            }
        } else if (q instanceof CruiseTrackQuery cq) {
            CruiseTrackResponse resp = listener.onCruiseTrackQuery(event.getSipId(), cq);
            if (resp != null) {
                ClientCommandSender.sendCruiseTrackResponse(from, to, resp);
            }
        } else if (q instanceof MobilePositionQuery mq) {
            MobilePositionNotify resp = listener.onMobilePositionQuery(event.getSipId(), mq);
            if (resp != null) {
                ClientCommandSender.sendMobilePositionNotify(from, to, resp);
            }
        } else if (q instanceof PresetQuery pq) {
            PresetQueryResponse resp = listener.onPresetQuery(event.getSipId(), pq);
            if (resp != null) {
                ClientCommandSender.sendPresetQueryResponse(from, to, resp);
            }
        } else if (q instanceof DeviceConfigDownload dcd) {
            DeviceConfigResponse resp = listener.onConfigDownloadQuery(event.getSipId(), dcd);
            if (resp != null) {
                if (resp.getSn() == null) {
                    resp.setSn(dcd.getSn());
                }
                ClientCommandSender.sendDeviceConfigCommand(from, to, resp);
            }
        } else {
            log.debug("未识别的查询类型: {}", q.getClass().getSimpleName());
        }
    }

    private void handleDeviceQuery(QueryListener listener, ClientQueryEvent event, DeviceQuery dq) {
        FromDevice from = supplier.getClientFromDevice();
        if (from == null) {
            log.warn("ClientDeviceSupplier.getClientFromDevice() 返回 null，无法回包: cmdType={}", dq.getCmdType());
            return;
        }
        ToDevice to = requireToDevice(event.getSipId());
        if (to == null) {
            return;
        }
        String cmdType = dq.getCmdType();
        if (cmdType == null) {
            log.debug("DeviceQuery cmdType 为空，跳过");
            return;
        }
        switch (cmdType) {
            case "Catalog" -> {
                DeviceResponse resp = listener.onCatalogQuery(event.getSipId(), dq);
                if (resp != null) {
                    if (resp.getSn() == null) {
                        resp.setSn(dq.getSn());
                    }
                    ClientCommandSender.sendCatalogCommand(from, to, resp);
                }
            }
            case "DeviceInfo" -> {
                DeviceInfo resp = listener.onDeviceInfoQuery(event.getSipId(), dq);
                if (resp != null) {
                    if (resp.getSn() == null) {
                        resp.setSn(dq.getSn());
                    }
                    ClientCommandSender.sendDeviceInfoCommand(from, to, resp);
                }
            }
            case "DeviceStatus" -> {
                DeviceStatus resp = listener.onDeviceStatusQuery(event.getSipId(), dq);
                if (resp != null) {
                    if (resp.getSn() == null) {
                        resp.setSn(dq.getSn());
                    }
                    ClientCommandSender.sendDeviceStatusCommand(from, to, resp);
                }
            }
            default -> log.debug("未识别的 DeviceQuery cmdType: {}", cmdType);
        }
    }

    private void warnQueryListenerMissingOnce() {
        if (queryListenerMissingWarned.compareAndSet(false, true)) {
            log.warn("收到 ClientQueryEvent 但未找到唯一的 QueryListener bean。"
                    + "请检查业务侧 listener 是否：(1) 标注 @Component / @Service / @Bean；"
                    + "(2) 落在 @SpringBootApplication.scanBasePackages 路径内；"
                    + "(3) 没有被 @ConditionalOnProperty 等条件过滤；"
                    + "(4) 注册数量为 1（>=2 个会让 ObjectProvider#getIfUnique 返回 null）。"
                    + "本进程内同类告警仅打一次；本次及后续查询将走默认空响应（无回包）。");
        }
    }

    /**
     * 强转 supplier.getDevice(sipId) 为 ToDevice，转换失败时给明确错误信息而非裸 ClassCastException。
     * 业务方覆写 ClientDeviceSupplier.getDevice 必须返回 ToDevice 子类型。
     */
    private ToDevice requireToDevice(String sipId) {
        Device device = supplier.getDevice(sipId);
        if (device == null) {
            log.warn("ClientDeviceSupplier.getDevice({}) 返回 null，无法回包", sipId);
            return null;
        }
        if (device instanceof ToDevice toDevice) {
            return toDevice;
        }
        // 其它实现（比如返回的是抽象 Device）走 supplier.getToDevice 转换
        ToDevice converted = supplier.getToDevice(device);
        if (converted == null) {
            log.warn("ClientDeviceSupplier.getToDevice({}) 返回 null，无法回包", sipId);
        }
        return converted;
    }

    // ============ Control 分发 ============

    @EventListener
    public void dispatch(ClientControlEvent event) {
        List<ControlListener> listeners = safeList(controlListeners);
        if (listeners.isEmpty()) {
            return;
        }
        DeviceControlBase cmd = event.getCommand();
        for (ControlListener l : listeners) {
            // 13 个 control 类互为兄弟节点，顺序无关
            if (cmd instanceof DeviceControlPtz c) {
                l.onPtzControl(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlTeleBoot c) {
                l.onTeleBoot(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlRecordCmd c) {
                l.onRecord(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlGuard c) {
                l.onGuard(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlAlarm c) {
                l.onAlarmReset(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlIFame c) {
                l.onIFrame(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlDragIn c) {
                l.onDragIn(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlDragOut c) {
                l.onDragOut(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlPosition c) {
                l.onHomePositionControl(event.getUserId(), c);
            } else if (cmd instanceof DeviceUpgradeControl c) {
                l.onDeviceUpgrade(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlPTZPrecise c) {
                l.onPtzPrecise(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlSDCardFormat c) {
                l.onFormatSdCard(event.getUserId(), c);
            } else if (cmd instanceof DeviceControlTargetTrack c) {
                l.onTargetTrack(event.getUserId(), c);
            } else {
                l.onUnknownControl(event.getUserId(), cmd);
            }
        }
    }

    @EventListener
    public void dispatch(ClientKeepaliveEvent event) {
        List<ControlListener> listeners = safeList(controlListeners);
        for (ControlListener l : listeners) {
            l.onKeepalive(event.getUserId(), event.getKeepalive());
        }
    }

    // ============ Config 分发（Class<?> → Consumer 映射表）============

    private record ConfigDispatchCtx(String userId, DeviceControlBase cfg) {}

    private static final Map<Class<?>, BiConsumer<ConfigListener, ConfigDispatchCtx>> CONFIG_DISPATCH = Map.ofEntries(
            Map.entry(DeviceConfigControl.class,
                    (l, c) -> l.onBasicParamConfig(c.userId, (DeviceConfigControl) c.cfg)),
            Map.entry(VideoParamOptConfig.class,
                    (l, c) -> l.onVideoParamOptConfig(c.userId, (VideoParamOptConfig) c.cfg)),
            Map.entry(SVACEncodeConfig.class,
                    (l, c) -> l.onSvacEncodeConfig(c.userId, (SVACEncodeConfig) c.cfg)),
            Map.entry(SVACDecodeConfig.class,
                    (l, c) -> l.onSvacDecodeConfig(c.userId, (SVACDecodeConfig) c.cfg)),
            Map.entry(VideoParamAttributeConfig.class,
                    (l, c) -> l.onVideoParamAttributeConfig(c.userId, (VideoParamAttributeConfig) c.cfg)),
            Map.entry(VideoRecordPlanConfig.class,
                    (l, c) -> l.onVideoRecordPlanConfig(c.userId, (VideoRecordPlanConfig) c.cfg)),
            Map.entry(VideoAlarmRecordConfig.class,
                    (l, c) -> l.onVideoAlarmRecordConfig(c.userId, (VideoAlarmRecordConfig) c.cfg)),
            Map.entry(PictureMaskConfig.class,
                    (l, c) -> l.onPictureMaskConfig(c.userId, (PictureMaskConfig) c.cfg)),
            Map.entry(FrameMirrorConfig.class,
                    (l, c) -> l.onFrameMirrorConfig(c.userId, (FrameMirrorConfig) c.cfg)),
            Map.entry(AlarmReportConfig.class,
                    (l, c) -> l.onAlarmReportConfig(c.userId, (AlarmReportConfig) c.cfg)),
            Map.entry(OsdConfig.class,
                    (l, c) -> l.onOsdConfig(c.userId, (OsdConfig) c.cfg)),
            Map.entry(SnapShotConfig.class,
                    (l, c) -> l.onSnapShotConfig(c.userId, (SnapShotConfig) c.cfg))
    );

    @EventListener
    public void dispatch(ClientConfigEvent event) {
        List<ConfigListener> listeners = safeList(configListeners);
        if (listeners.isEmpty()) {
            return;
        }
        DeviceControlBase cfg = event.getConfig();
        BiConsumer<ConfigListener, ConfigDispatchCtx> dispatcher = CONFIG_DISPATCH.get(cfg.getClass());
        if (dispatcher == null) {
            for (ConfigListener l : listeners) {
                l.onUnknownConfig(event.getUserId(), cfg);
            }
            return;
        }
        ConfigDispatchCtx ctx = new ConfigDispatchCtx(event.getUserId(), cfg);
        for (ConfigListener l : listeners) {
            dispatcher.accept(l, ctx);
        }
    }

    /** 暴露给单元测试断言映射表完整性。 */
    public static java.util.Set<Class<?>> getConfigDispatchKeys() {
        return CONFIG_DISPATCH.keySet();
    }

    // ============ Subscribe 分发（fire-and-forget）============

    @EventListener
    public void dispatch(ClientSubscribeEvent event) {
        List<SubscribeListener> listeners = safeList(subscribeListeners);
        if (listeners.isEmpty()) {
            return;
        }
        XmlBean body = event.getBody();
        for (SubscribeListener l : listeners) {
            if (body instanceof DeviceQuery dq && "Catalog".equals(dq.getCmdType())) {
                l.onCatalogSubscribe(event.getSipId(), event.getExpires(), dq);
            } else if (body instanceof DeviceAlarmQuery aq) {
                l.onAlarmSubscribe(event.getSipId(), event.getExpires(), aq);
            } else if (body instanceof DeviceMobileQuery mq) {
                l.onMobilePositionSubscribe(event.getSipId(), event.getExpires(), mq);
            } else if (body instanceof PTZPositionQuery pq) {
                l.onPtzPositionSubscribe(event.getSipId(), event.getExpires(), pq);
            } else {
                log.debug("未识别的订阅 body: {}", body == null ? "null" : body.getClass().getSimpleName());
            }
        }
    }

    // ============ Notify 分发 ============

    @EventListener
    public void dispatch(ClientNotifyEvent event) {
        List<NotifyListener> listeners = safeList(notifyListeners);
        if (listeners.isEmpty()) {
            return;
        }
        for (NotifyListener l : listeners) {
            if (event.getNotify() instanceof DeviceBroadcastNotify n) {
                l.onBroadcastNotify(event.getUserId(), n);
            } else {
                l.onUnknownNotify(event.getUserId(), event.getNotify());
            }
        }
    }

    // ============ Helpers ============

    private static <T> List<T> safeList(ObjectProvider<List<T>> provider) {
        List<T> list = provider.getIfAvailable();
        return list != null ? list : List.of();
    }
}
