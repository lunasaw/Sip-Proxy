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

    /**
     * GB28181-2022 A.2.5.9 设备软件升级结果通知
     */
    DEVICE_UPGRADE_RESULT("DeviceUpgradeResult", "设备软件升级结果通知"),

    /**
     * GB28181-2022 A.2.5.7 图像抓拍传输完成通知
     */
    UPLOAD_SNAP_SHOT_FINISHED("UploadSnapShotFinished", "图像抓拍传输完成通知"),

    /**
     * GB28181-2022 A.2.4.13 / A.2.6.15 PTZ 精确状态查询/应答
     */
    PTZ_POSITION("PTZPosition", "PTZ 精确状态查询/应答"),

    /**
     * GB28181-2022 A.2.4.14 / A.2.6.16 存储卡状态查询/应答
     */
    SD_CARD_STATUS("SDCardStatus", "存储卡状态查询/应答"),

    /**
     * GB28181-2022 A.2.4.10 / A.2.6.12 看守位信息查询/应答
     */
    HOME_POSITION_QUERY("HomePositionQuery", "看守位信息查询/应答"),

    /**
     * GB28181-2022 A.2.4.11 / A.2.6.13 巡航轨迹列表查询/应答
     */
    CRUISE_TRACK_LIST_QUERY("CruiseTrackListQuery", "巡航轨迹列表查询/应答"),

    /**
     * GB28181-2022 A.2.4.12 / A.2.6.14 巡航轨迹查询/应答
     */
    CRUISE_TRACK_QUERY("CruiseTrackQuery", "巡航轨迹查询/应答"),


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
