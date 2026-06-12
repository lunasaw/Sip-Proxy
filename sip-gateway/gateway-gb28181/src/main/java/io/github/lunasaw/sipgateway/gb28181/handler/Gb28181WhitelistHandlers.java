package io.github.lunasaw.sipgateway.gb28181.handler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlTargetTrack;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.AuxiliaryControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.CruiseControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.FIControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PresetControlEnum;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.ScanControlEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sipgateway.core.api.CommandMapping;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * GB28181 注解白名单处理器：覆盖有重载、强制字段、默认值、Date 类型转换等复杂命令。
 *
 * <p>方法签名约束：{@code (GatewayCommand) → String}（返回 correlationId）
 *
 * @author luna
 */
@Component
@RequiredArgsConstructor
public class Gb28181WhitelistHandlers {

    private final ServerCommandSender sender;

    // ============== Query ==============

    @CommandMapping("gb28181.Query.RecordInfo")
    public String recordInfo(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceRecordInfoQuery(cmd.deviceId(),
                ((Number) requireField(p, "startTime", cmd.type())).longValue(),
                ((Number) requireField(p, "endTime", cmd.type())).longValue());
    }

    @CommandMapping("gb28181.Query.AlarmQuery")
    public String alarmQuery(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceAlarmQuery(cmd.deviceId(),
                new Date(((Number) requireField(p, "startTime", cmd.type())).longValue()),
                new Date(((Number) requireField(p, "endTime", cmd.type())).longValue()),
                (String) p.get("startPriority"),
                (String) p.get("endPriority"),
                (String) p.get("alarmMethod"),
                (String) p.get("alarmType"));
    }

    // ============== Subscribe ==============

    @CommandMapping("gb28181.Subscribe.MobilePosition")
    public String mobilePositionSubscribe(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceMobilePositionSubscribe(cmd.deviceId(),
                (String) requireField(p, "interval", cmd.type()),
                ((Number) requireField(p, "expires", cmd.type())).intValue(),
                (String) requireField(p, "eventType", cmd.type()),
                (String) p.get("eventId"));
    }

    @CommandMapping("gb28181.Subscribe.Alarm")
    public String alarmSubscribe(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceAlarmSubscribe(cmd.deviceId(),
                ((Number) requireField(p, "expires", cmd.type())).intValue(),
                (String) requireField(p, "eventType", cmd.type()),
                (String) p.get("startPriority"),
                (String) p.get("endPriority"),
                (String) p.get("alarmMethod"),
                (String) p.get("alarmType"),
                (String) p.get("startAlarmTime"),
                (String) p.get("endAlarmTime"));
    }

    @CommandMapping("gb28181.Subscribe.Refresh")
    public String refresh(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        String callId = (String) requireField(p, "callId", cmd.type());
        int expires = ((Number) requireField(p, "expires", cmd.type())).intValue();
        if (p.containsKey("content")) {
            return sender.refreshSubscribe(callId, (String) p.get("content"), expires);
        }
        return sender.refreshSubscribe(callId, expires);
    }

    // ============== Control ==============

    @CommandMapping("gb28181.Control.Ptz")
    public String ptz(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        if (p.containsKey("hex")) {
            return sender.deviceControlPtzCmd(cmd.deviceId(), (String) p.get("hex"));
        }
        return sender.deviceControlPtzCmd(cmd.deviceId(),
                JSON.to(PTZControlEnum.class, requireField(p, "cmd", cmd.type())),
                ((Number) p.getOrDefault("speed", 128)).intValue());
    }

    @CommandMapping("gb28181.Control.AlarmReset")
    public String alarmReset(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlAlarm(cmd.deviceId(), "ResetAlarm",
                (String) p.get("alarmMethod"), (String) p.get("alarmType"));
    }

    @CommandMapping("gb28181.Control.FI")
    public String fi(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlFI(cmd.deviceId(),
                JSON.to(FIControlEnum.IrisDirection.class, requireField(p, "iris", cmd.type())),
                JSON.to(FIControlEnum.FocusDirection.class, requireField(p, "focus", cmd.type())),
                ((Number) p.getOrDefault("focusSpeed", 128)).intValue(),
                ((Number) p.getOrDefault("irisSpeed", 128)).intValue());
    }

    @CommandMapping("gb28181.Control.Preset")
    public String preset(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlPreset(cmd.deviceId(),
                JSON.to(PresetControlEnum.class, requireField(p, "cmd", cmd.type())),
                ((Number) requireField(p, "presetIndex", cmd.type())).intValue());
    }

    @CommandMapping("gb28181.Control.Cruise")
    public String cruise(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlCruise(cmd.deviceId(),
                JSON.to(CruiseControlEnum.class, requireField(p, "cmd", cmd.type())),
                ((Number) requireField(p, "groupNumber", cmd.type())).intValue(),
                ((Number) requireField(p, "presetIndex", cmd.type())).intValue());
    }

    @CommandMapping("gb28181.Control.CruiseSpeedOrTime")
    public String cruiseSpeedOrTime(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlCruiseSpeedOrTime(cmd.deviceId(),
                JSON.to(CruiseControlEnum.class, requireField(p, "cmd", cmd.type())),
                ((Number) requireField(p, "groupNumber", cmd.type())).intValue(),
                ((Number) requireField(p, "presetNumber", cmd.type())).intValue(),
                ((Number) requireField(p, "speedOrTime", cmd.type())).intValue());
    }

    @CommandMapping("gb28181.Control.Scan")
    public String scan(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlScan(cmd.deviceId(),
                ((Number) requireField(p, "groupNumber", cmd.type())).intValue(),
                JSON.to(ScanControlEnum.ScanOperationType.class,
                        p.getOrDefault("operationType", "ON")));
    }

    @CommandMapping("gb28181.Control.Auxiliary")
    public String auxiliary(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlAuxiliary(cmd.deviceId(),
                JSON.to(AuxiliaryControlEnum.class, requireField(p, "cmd", cmd.type())),
                ((Number) requireField(p, "switchNumber", cmd.type())).intValue());
    }

    @CommandMapping("gb28181.Control.TargetTrack")
    public String targetTrack(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        DeviceControlTargetTrack.TargetArea area = p.get("targetArea") != null
                ? JSON.to(DeviceControlTargetTrack.TargetArea.class, p.get("targetArea"))
                : null;
        return sender.deviceControlTargetTrack(cmd.deviceId(),
                (String) requireField(p, "mode", cmd.type()),
                (String) requireField(p, "deviceId2", cmd.type()),
                area);
    }

    // ============== Invite ==============

    @CommandMapping("gb28181.Invite.Play")
    public String invitePlay(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInvitePlay(cmd.deviceId(),
                (String) p.get("channelId"),
                (String) requireField(p, "mediaIp", cmd.type()),
                ((Number) requireField(p, "mediaPort", cmd.type())).intValue(),
                streamMode(p));
    }

    @CommandMapping("gb28181.Invite.Playback")
    public String invitePlayback(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInvitePlayBack(cmd.deviceId(),
                (String) requireField(p, "mediaIp", cmd.type()),
                ((Number) requireField(p, "mediaPort", cmd.type())).intValue(),
                streamMode(p),
                (String) requireField(p, "startTime", cmd.type()),
                (String) requireField(p, "endTime", cmd.type()));
    }

    @CommandMapping("gb28181.Invite.Talk")
    public String inviteTalk(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInviteTalk(cmd.deviceId(),
                (String) requireField(p, "mediaIp", cmd.type()),
                ((Number) requireField(p, "mediaPort", cmd.type())).intValue(),
                streamMode(p));
    }

    @CommandMapping("gb28181.Invite.Download")
    public String inviteDownload(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInviteDownload(cmd.deviceId(),
                (String) requireField(p, "mediaIp", cmd.type()),
                ((Number) requireField(p, "mediaPort", cmd.type())).intValue(),
                streamMode(p),
                (String) requireField(p, "startTime", cmd.type()),
                (String) requireField(p, "endTime", cmd.type()),
                ((Number) requireField(p, "downloadSpeed", cmd.type())).intValue());
    }

    @CommandMapping("gb28181.Invite.PlaybackControl")
    public String invitePlaybackControl(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInvitePlayBackControl(cmd.deviceId(),
                JSON.to(PlayActionEnums.class, requireField(p, "action", cmd.type())));
    }

    @CommandMapping("gb28181.Invite.Ack")
    public String ack(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        String callId = (String) p.get("callId");
        return callId != null ? sender.deviceAck(cmd.deviceId(), callId) : sender.deviceAck(cmd.deviceId());
    }

    private static StreamModeEnum streamMode(Map<String, Object> p) {
        Object raw = p.getOrDefault("streamMode", "UDP");
        if (raw instanceof StreamModeEnum) {
            return (StreamModeEnum) raw;
        }
        return StreamModeEnum.valueOf(raw.toString());
    }

    private static Object requireField(Map<String, Object> payload, String field, String type) {
        Object v = payload.get(field);
        if (v == null) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "missing field: " + field + " for type " + type);
        }
        return v;
    }
}
