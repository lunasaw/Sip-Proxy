package io.github.lunasaw.sipgateway.gb28181.handler;

import io.github.lunasaw.gb28181.common.entity.control.DragZoom;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sipgateway.core.api.CommandSpec;
import io.github.lunasaw.sipgateway.core.api.ParamBinding;

import java.util.List;

/**
 * GB28181 命令静态映射表。
 *
 * <p><strong>判定规则</strong>（决定走表 vs 走注解）：
 * <ul>
 *   <li>"deviceId + ≤4 个标量/无重载/无默认值/无强制字段" → 走表</li>
 *   <li>有重载 / 强制字段 / 默认值 / Date 类型转换 → 走 @CommandMapping</li>
 * </ul>
 *
 * <p>所有 type 严格以 "gb28181." 开头，启动期 Registry 校验。
 *
 * @author luna
 */
public final class Gb28181CommandSpecs {

    private Gb28181CommandSpecs() {}

    public static List<CommandSpec> declare() {
        return List.of(
                // ===== Query (10 simple) =====
                spec("gb28181.Query.DeviceInfo", "deviceInfoQuery", arg("deviceId")),
                spec("gb28181.Query.DeviceStatus", "deviceStatusQuery", arg("deviceId")),
                spec("gb28181.Query.Catalog", "deviceCatalogQuery", arg("deviceId")),
                spec("gb28181.Query.PresetQuery", "devicePresetQuery", arg("deviceId")),
                spec("gb28181.Query.MobilePosition", "deviceMobilePositionQuery", arg("deviceId"), arg("interval")),
                spec("gb28181.Query.PtzPosition", "devicePtzPositionQuery", arg("deviceId")),
                spec("gb28181.Query.SdCardStatus", "deviceSdCardStatusQuery", arg("deviceId")),
                spec("gb28181.Query.HomePosition", "deviceHomePositionQuery", arg("deviceId")),
                spec("gb28181.Query.CruiseTrackList", "deviceCruiseTrackListQuery", arg("deviceId")),
                spec("gb28181.Query.CruiseTrack", "deviceCruiseTrackQuery", arg("deviceId"), arg("number:int")),

                // ===== Subscribe (3 simple) =====
                spec("gb28181.Subscribe.Catalog", "deviceCatalogSubscribe",
                        arg("deviceId"), arg("expires:int"), arg("eventType")),
                spec("gb28181.Subscribe.PtzPosition", "devicePtzPositionSubscribe",
                        arg("deviceId"), arg("expires:int")),
                spec("gb28181.Subscribe.Unsubscribe", "unsubscribe", arg("callId")),

                // ===== Control (8 simple) =====
                spec("gb28181.Control.Reboot", "deviceControlReboot", arg("deviceId")),
                spec("gb28181.Control.Record", "deviceControlRecord", arg("deviceId"), arg("recordCmd")),
                spec("gb28181.Control.Guard", "deviceControlGuardCmd", arg("deviceId"), arg("guardCmd")),
                spec("gb28181.Control.IFrame", "deviceControlIFrame", arg("deviceId")),
                spec("gb28181.Control.HomePosition", "deviceControlHomePosition",
                        arg("deviceId"), arg("enabled"), arg("resetTime"), arg("presetIndex")),
                spec("gb28181.Control.PtzPrecise", "deviceControlPtzPrecise",
                        arg("deviceId"), arg("pan:double"), arg("tilt:double"), arg("zoom:double")),
                spec("gb28181.Control.FormatSDCard", "deviceControlFormatSDCard",
                        arg("deviceId"), arg("sdNumber:int")),
                spec("gb28181.Control.ScanSpeed", "deviceControlScanSpeed",
                        arg("deviceId"), arg("groupNumber:int"), arg("speed:int")),
                spec("gb28181.Control.DragZoomIn", "deviceControlDragZoomIn",
                        arg("deviceId"),
                        new ParamBinding("payload", "dragZoom", DragZoom.class, null)),
                spec("gb28181.Control.DragZoomOut", "deviceControlDragZoomOut",
                        arg("deviceId"),
                        new ParamBinding("payload", "dragZoom", DragZoom.class, null)),

                // ===== Config (10 simple) =====
                spec("gb28181.Config.BasicParam", "deviceConfig",
                        arg("deviceId"), arg("name"), arg("expiration"),
                        arg("heartBeatInterval"), arg("heartBeatCount")),
                spec("gb28181.Config.Osd", "deviceConfigOsd",
                        arg("deviceId"),
                        new ParamBinding("payload", "osdInfo", OsdConfig.OsdInfo.class, null)),
                spec("gb28181.Config.ConfigDownload", "deviceConfigDownload",
                        arg("deviceId"), arg("configType")),

                // ===== Invite (1 simple) =====
                spec("gb28181.Invite.Bye", "deviceBye", arg("callId")),

                // ===== Device (3 simple) =====
                spec("gb28181.Device.Upgrade", "deviceUpgrade",
                        arg("deviceId"), arg("firmware"), arg("fileURL"),
                        arg("manufacturer"), arg("sessionId")),
                spec("gb28181.Device.SnapShot", "deviceSnapShot",
                        arg("deviceId"), arg("snapNum:int"), arg("interval:int"),
                        arg("uploadURL"), arg("sessionId")),
                spec("gb28181.Device.Broadcast", "deviceBroadcast", arg("deviceId"))
        );
    }

    private static CommandSpec spec(String type, String methodName, ParamBinding... bindings) {
        return new CommandSpec(type, ServerCommandSender.class, methodName, List.of(bindings));
    }

    /**
     * 简化参数绑定的工厂方法，调用 {@link ParamBinding#parse}。
     */
    private static ParamBinding arg(String dsl) {
        return ParamBinding.parse(dsl);
    }
}
