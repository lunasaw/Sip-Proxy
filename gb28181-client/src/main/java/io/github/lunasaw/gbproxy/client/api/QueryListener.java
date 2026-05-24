package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.query.ConfigDownloadQuery;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackListQuery;
import io.github.lunasaw.gb28181.common.entity.query.CruiseTrackQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceAlarmQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceConfigDownload;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.query.DeviceRecordQuery;
import io.github.lunasaw.gb28181.common.entity.query.HomePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.MobilePositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PTZPositionQuery;
import io.github.lunasaw.gb28181.common.entity.query.PresetQuery;
import io.github.lunasaw.gb28181.common.entity.query.SDCardStatusQuery;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.response.ConfigDownloadResponse;
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

/**
 * 平台查询监听器（client 角色业务方实现）。
 *
 * <p>方法返回非 null = 框架自动调用 {@code ClientCommandSender.sendXxxResponse}
 * 回包给平台。返回 null = 不回包（业务方决定不响应）。
 *
 * <p>多 listener 处理策略：
 * <ul>
 *   <li>业务方注册 0 个 QueryListener bean：所有查询走默认空响应（无回包），并在首次告警一次</li>
 *   <li>业务方注册 1 个 QueryListener bean：正常分发</li>
 *   <li>业务方注册 ≥2 个 QueryListener bean：Spring 启动期 fail fast（{@code getIfUnique()} 返回 null）</li>
 * </ul>
 *
 * <p>注意点：
 * <ol>
 *   <li>业务方 bean 必须落在 {@code @SpringBootApplication.scanBasePackages} 路径之下，
 *       否则 Adapter 取不到 listener。</li>
 *   <li>方法都带 {@code default} 实现，业务方按需 override，不写一行空方法。</li>
 * </ol>
 *
 * @author luna
 */
public interface QueryListener {

    /** 平台目录查询（cmdType=Catalog）。 */
    default DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        return null;
    }

    /** 平台设备信息查询（cmdType=DeviceInfo）。 */
    default DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        return null;
    }

    /** 平台设备状态查询（cmdType=DeviceStatus）。 */
    default DeviceStatus onDeviceStatusQuery(String platformId, DeviceQuery query) {
        return null;
    }

    /** 平台录像信息查询（cmdType=RecordInfo）。 */
    default DeviceRecord onRecordInfoQuery(String platformId, DeviceRecordQuery query) {
        return null;
    }

    /** 平台告警查询（cmdType=Alarm）。 */
    default DeviceAlarmNotify onAlarmQuery(String platformId, DeviceAlarmQuery query) {
        return null;
    }

    /**
     * 平台配置下载查询（cmdType=ConfigDownload，旧形态：DeviceConfigDownload）。
     * 由 {@code ConfigDownloadMessageHandler} 路径触发，返回 {@code DeviceConfigResponse}。
     */
    default DeviceConfigResponse onConfigDownloadQuery(String platformId, DeviceConfigDownload query) {
        return null;
    }

    /**
     * 平台配置下载查询（cmdType=ConfigDownload，新形态：ConfigDownloadQuery）。
     * 由 {@code ConfigDownloadQueryMessageClientHandler} 路径触发，返回 {@code ConfigDownloadResponse}。
     * 与 {@link #onConfigDownloadQuery(String, DeviceConfigDownload)} 是同一 cmdType 的两个并存
     * 实现路径，沿用历史并行结构（pre-existing duplicate registration）。
     */
    default ConfigDownloadResponse onConfigDownloadQueryV2(String platformId, ConfigDownloadQuery query) {
        return null;
    }

    /** 平台预置位查询（cmdType=Preset）。 */
    default PresetQueryResponse onPresetQuery(String platformId, PresetQuery query) {
        return null;
    }

    /** 平台移动位置查询（cmdType=MobilePosition）。 */
    default MobilePositionNotify onMobilePositionQuery(String platformId, MobilePositionQuery query) {
        return null;
    }

    /** GB28181-2022 §A.2.4.13 PTZ 精确状态查询（cmdType=PTZPosition）。 */
    default PTZPositionResponse onPtzPositionQuery(String platformId, PTZPositionQuery query) {
        return null;
    }

    /** GB28181-2022 §A.2.4.14 SD 卡状态查询（cmdType=SDCardStatus）。 */
    default SDCardStatusResponse onSdCardStatusQuery(String platformId, SDCardStatusQuery query) {
        return null;
    }

    /** GB28181-2022 §A.2.4.15 看守位查询（cmdType=HomePosition）。 */
    default HomePositionResponse onHomePositionQuery(String platformId, HomePositionQuery query) {
        return null;
    }

    /** GB28181-2022 §A.2.4.16 巡航轨迹列表查询（cmdType=CruiseTrackList）。 */
    default CruiseTrackListResponse onCruiseTrackListQuery(String platformId, CruiseTrackListQuery query) {
        return null;
    }

    /** GB28181-2022 §A.2.4.17 巡航轨迹查询（cmdType=CruiseTrack）。 */
    default CruiseTrackResponse onCruiseTrackQuery(String platformId, CruiseTrackQuery query) {
        return null;
    }
}
