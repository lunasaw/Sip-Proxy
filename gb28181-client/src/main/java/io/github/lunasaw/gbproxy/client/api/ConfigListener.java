package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.control.DeviceConfigControl;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
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

/**
 * 平台配置监听器（cmdType=DeviceConfig 的 12 个子标签，client 角色业务方实现）。
 *
 * <p>fire-and-forget：所有方法返回 void。
 *
 * <p>12 个 config 类全部直接 extends {@code DeviceControlBase}，互为兄弟节点。
 * Adapter 用 {@code Class<?> → Consumer} 显式映射表分发，规避未来父子化重构时的
 * instanceof 顺序陷阱，同时支持单元测试遍历断言完整性。
 *
 * @author luna
 */
public interface ConfigListener {

    /** GB28181-2022 §A.2.3.2.1 基本参数配置（BasicParam，承载在 {@code DeviceConfigControl}）。 */
    default void onBasicParamConfig(String platformId, DeviceConfigControl cfg) {}

    /** GB28181-2022 §A.2.3.2.2 视频参数范围配置（VideoParamOpt）。 */
    default void onVideoParamOptConfig(String platformId, VideoParamOptConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.3 SVAC 编码配置（SVACEncodeConfig）。 */
    default void onSvacEncodeConfig(String platformId, SVACEncodeConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.4 SVAC 解码配置（SVACDecodeConfig）。 */
    default void onSvacDecodeConfig(String platformId, SVACDecodeConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.5 视频参数属性配置（VideoParamAttribute）。 */
    default void onVideoParamAttributeConfig(String platformId, VideoParamAttributeConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.6 录像计划配置（VideoRecordPlan）。 */
    default void onVideoRecordPlanConfig(String platformId, VideoRecordPlanConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.7 视频报警录像配置（VideoAlarmRecord）。 */
    default void onVideoAlarmRecordConfig(String platformId, VideoAlarmRecordConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.8 视频遮挡区域配置（PictureMask）。 */
    default void onPictureMaskConfig(String platformId, PictureMaskConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.9 画面镜像配置（FrameMirror）。 */
    default void onFrameMirrorConfig(String platformId, FrameMirrorConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.10 报警上报配置（AlarmReport）。 */
    default void onAlarmReportConfig(String platformId, AlarmReportConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.11 OSD 配置（OSDConfig）。 */
    default void onOsdConfig(String platformId, OsdConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.12 抓拍参数配置（SnapShotConfig）。 */
    default void onSnapShotConfig(String platformId, SnapShotConfig cfg) {}

    /**
     * 框架内部分发兜底：当 ClientConfigEvent 携带的 config 类不属于上述 12 类时调用。
     * 业务方一般无需 override；保留此 hook 用于诊断和未来 config 子类的扩展。
     */
    default void onUnknownConfig(String platformId, DeviceControlBase cfg) {}
}

