package io.github.lunasaw.gbproxy.server.api;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackListResponse;
import io.github.lunasaw.gb28181.common.entity.response.CruiseTrackResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceConfigResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gb28181.common.entity.response.HomePositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.PTZPositionResponse;
import io.github.lunasaw.gb28181.common.entity.response.SDCardStatusResponse;

/**
 * 设备应答监听器（server 角色业务方实现）。
 *
 * <p>对应 GB28181 设备主动应答 query 类请求 + 错误返回。fire-and-forget。
 *
 * @author luna
 */
public interface DeviceResponseListener {

    /** 设备目录应答（cmdType=Catalog）。 */
    default void onCatalogResponse(String deviceId, String sn, DeviceResponse catalog) {}

    /** 设备信息应答（cmdType=DeviceInfo）。 */
    default void onDeviceInfoResponse(String deviceId, String sn, DeviceInfo info) {}

    /** 设备信息应答错误（cmdType=DeviceInfo 处理异常）。 */
    default void onDeviceInfoError(String deviceId, String reason) {}

    /** 设备信息查询请求（INFO 类型 SIP 请求）。 */
    default void onDeviceInfoRequest(String deviceId, String content) {}

    /** 设备状态应答（cmdType=DeviceStatus）。 */
    default void onDeviceStatusResponse(String deviceId, String sn, DeviceStatus status) {}

    /** 设备录像信息应答（cmdType=RecordInfo）。 */
    default void onRecordInfoResponse(String deviceId, String sn, DeviceRecord record) {}

    /** PTZ 精确位置应答（cmdType=PTZPosition）。 */
    default void onPtzPositionResponse(String deviceId, PTZPositionResponse response) {}

    /** SD 卡状态应答（cmdType=SDCardStatus）。 */
    default void onSdCardStatusResponse(String deviceId, SDCardStatusResponse response) {}

    /** 看守位应答（cmdType=HomePositionQuery）。 */
    default void onHomePositionResponse(String deviceId, HomePositionResponse response) {}

    /** 巡航轨迹列表应答（cmdType=CruiseTrackListQuery）。 */
    default void onCruiseTrackListResponse(String deviceId, CruiseTrackListResponse response) {}

    /** 巡航轨迹应答（cmdType=CruiseTrackQuery）。 */
    default void onCruiseTrackResponse(String deviceId, CruiseTrackResponse response) {}

    /** 设备配置应答（cmdType=DeviceConfig）。 */
    default void onConfigResponse(String deviceId, String sn, DeviceConfigResponse response) {}

    /** 设备订阅应答（method=SUBSCRIBE，server 收到 200 OK 等）。 */
    default void onSubscribeResponse(String deviceId, String callId, int statusCode) {}

    /** 设备 Catalog NOTIFY 中的通道更新（含 ON/OFF/ADD/DEL/UPDATE）。 */
    default void onNotifyUpdate(String deviceId, DeviceOtherUpdateNotify notify) {}
}
