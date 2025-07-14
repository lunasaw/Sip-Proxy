package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * @author luna
 * @date 2023/10/13
 */
public enum CmdTypeEnum {

    /**
     * server请求类型
     */
    DEVICE_INFO("DeviceInfo", "查询设备信息"),
    DEVICE_STATUS("DeviceStatus", "查询设备状态"),
    CATALOG("Catalog", "设备目录"),
    RECORD_INFO("RecordInfo", "查询设备录像信息"),
    ALARM("Alarm", "查询设备告警信息"),
    CONFIG_DOWNLOAD("ConfigDownload", "设备配置下载"),
    PRESET_QUERY("PresetQuery", "查询预置位"),
    MOBILE_POSITION("MobilePosition", "移动位置信息"),
    DEVICE_CONTROL("DeviceControl", "设备控制"),
    BROADCAST("Broadcast", "设备广播"),
    DEVICE_CONFIG("DeviceConfig", "设备配置"),
    MEDIA_STATUS("MediaStatus", "媒体状态信息"),

    KEEPALIVE("Keepalive", "心跳"),


    // client


    REGISTER("REGISTER", "注册"),
    ;

    private final String type;
    private final String desc;

    CmdTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }


    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
