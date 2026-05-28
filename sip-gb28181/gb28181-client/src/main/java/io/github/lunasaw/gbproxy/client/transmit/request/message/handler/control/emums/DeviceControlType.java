package io.github.lunasaw.gbproxy.client.transmit.request.message.handler.control.emums;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import io.github.lunasaw.gb28181.common.entity.control.*;
import lombok.Getter;
import lombok.SneakyThrows;

/**
 * 设备控制命令类型枚举，定义 GB28181 DeviceControl 消息中各子命令标签与对应实体类的映射关系。
 *
 * @author luna
 */
@Getter
public enum DeviceControlType {

    /**
     * 云台控制
     * 上下左右，预置位，扫描，辅助功能，巡航
     */
    PTZ("PTZCmd", "云台控制", DeviceControlPtz.class, "ptzCmdControl"),
    /**
     * 远程启动
     */
    TELNET_BOOT("TeleBoot", "远程启动", DeviceControlTeleBoot.class, "telnetBootControl"),
    /**
     * 录像控制
     */
    RECORD("RecordCmd", "录像控制", DeviceControlRecordCmd.class, "recordCmdControl"),
    /**
     * 布防撤防
     */
    GUARD("GuardCmd", "布防撤防", DeviceControlGuard.class, "guardCmdControl"),
    /**
     * 告警控制
     */
    ALARM("AlarmCmd", "告警控制", DeviceControlAlarm.class, "alarmCmdControl"),
    /**
     * 强制关键帧
     */
    I_FRAME("IFameCmd", "强制关键帧", DeviceControlIFame.class, "iFameCmdControl"),
    /**
     * 拉框放大
     */
    DRAG_ZOOM_IN("DragZoomIn", "拉框放大", DeviceControlDragIn.class, "dragZoomInControl"),
    /**
     * 拉框缩小
     */
    DRAG_ZOOM_OUT("DragZoomOut", "拉框缩小", DeviceControlDragOut.class, "dragZoomOutControl"),
    /**
     * 看守位
     */
    HOME_POSITION("HomePosition", "看守位", DeviceControlPosition.class, "HomePositionControl");

    /** val 到枚举的快速查找表。 */
    private static final Map<String, DeviceControlType> MAP = new ConcurrentHashMap<>();

    static {
        // MAP 初始化
        for (DeviceControlType deviceControlType : DeviceControlType.values()) {
            MAP.put(deviceControlType.getVal(), deviceControlType);
        }
    }

    /** 命令标签值，对应 XML 子元素名称。 */
    private final String   val;
    /** 命令描述。 */
    private final String   desc;
    /** 对应的实体类。 */
    private final Class<?> clazz;
    /** Spring Bean 名称。 */
    @Setter
    private String         beanName;

    DeviceControlType(String val, String desc, Class<?> clazz, String beanName) {
        this.val = val;
        this.desc = desc;
        this.clazz = clazz;
        this.beanName = beanName;
    }

    /**
     * 根据消息内容模糊匹配控制类型（包含匹配）。
     *
     * @param content 消息内容字符串
     * @return 匹配的控制类型，未匹配时返回 null
     */
    public static DeviceControlType getDeviceControlTypeFilter(String content) {
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }
        String key = MAP.keySet().stream().filter(content::contains).findFirst().orElse(StringUtils.EMPTY);

        return getDeviceControlType(key);
    }

    /**
     * 根据标签值精确查找控制类型。
     *
     * @param key 命令标签值
     * @return 对应的控制类型，未找到时返回 null
     */
    @SneakyThrows
    public static DeviceControlType getDeviceControlType(String key) {
        if (key == null) {
            return null;
        }
        if (MAP.containsKey(key)) {
            return MAP.get(key);
        }
        return null;
    }

}
