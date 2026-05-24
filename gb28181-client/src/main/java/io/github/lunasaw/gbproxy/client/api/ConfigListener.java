package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.control.DeviceConfigControl;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlBase;
import io.github.lunasaw.gb28181.common.entity.control.SnapShotConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.AlarmReportConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.OsdConfig;
import io.github.lunasaw.gb28181.common.entity.control.cfg.VideoAlarmRecordConfig;

/**
 * 平台配置监听器（cmdType=DeviceConfig 的子集，client 角色业务方实现）。
 *
 * <p>fire-and-forget：所有方法返回 void。
 *
 * <p>5 个 config 类全部直接 extends {@code DeviceControlBase}，互为兄弟节点。
 * Adapter 用 {@code Class<?> → Consumer} 显式映射表分发，规避未来父子化重构时的
 * instanceof 顺序陷阱，同时支持单元测试遍历断言完整性。
 *
 * @author luna
 */
public interface ConfigListener {

    /** GB28181-2022 §A.2.3.2.12 抓拍参数配置（SnapShotConfig）。 */
    default void onSnapShotConfig(String platformId, SnapShotConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.11 OSD 配置（OSDConfig）。 */
    default void onOsdConfig(String platformId, OsdConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.10 报警上报配置（AlarmReport）。 */
    default void onAlarmReportConfig(String platformId, AlarmReportConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.7 视频报警录像配置（VideoAlarmRecord）。 */
    default void onVideoAlarmRecordConfig(String platformId, VideoAlarmRecordConfig cfg) {}

    /** GB28181-2022 §A.2.3.2.1 基本参数配置（BasicParam，承载在 {@code DeviceConfigControl}）。 */
    default void onBasicParamConfig(String platformId, DeviceConfigControl cfg) {}

    /**
     * 框架内部分发兜底：当 ClientConfigEvent 携带的 config 类不属于上述 5 类时调用。
     * 业务方一般无需 override；保留此 hook 用于诊断和未来 config 子类的扩展。
     */
    default void onUnknownConfig(String platformId, DeviceControlBase cfg) {}
}
