package io.github.lunasaw.gbproxy.server.api;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UpgradeResultNotify;
import io.github.lunasaw.gb28181.common.entity.notify.UploadSnapShotFinishedNotify;

/**
 * 设备主动通知监听器（server 角色业务方实现）。
 *
 * <p>对应设备 → 平台方向的主动 NOTIFY / 异常上报，业务方一般据此做实时通知、计数、入库等。
 * fire-and-forget。
 *
 * @author luna
 */
public interface DeviceNotifyListener {

    /** 设备告警通知（cmdType=Alarm）。 */
    default void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {}

    /** 设备心跳通知（cmdType=Keepalive，承载 keepalive 状态）。 */
    default void onKeepalive(String deviceId, DeviceKeepLiveNotify notify) {}

    /** 流媒体状态通知（cmdType=MediaStatus）。 */
    default void onMediaStatus(String deviceId, MediaStatusNotify notify) {}

    /** 移动位置通知（cmdType=MobilePosition）。 */
    default void onMobilePositionNotify(String deviceId, MobilePositionNotify notify) {}

    /** 设备升级结果通知（GB28181-2022 §A.2.6.1）。 */
    default void onUpgradeResult(String deviceId, UpgradeResultNotify notify) {}

    /** 抓拍完成通知（GB28181-2022 §A.2.6.2）。 */
    default void onSnapShotFinished(String deviceId, UploadSnapShotFinishedNotify notify) {}
}
