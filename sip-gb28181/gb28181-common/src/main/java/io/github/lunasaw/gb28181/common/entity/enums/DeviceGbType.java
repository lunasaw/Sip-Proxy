package io.github.lunasaw.gb28181.common.entity.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * GBT-28181 设备 / 目录项类型代码（DeviceID 的第 11-13 位）。
 *
 * <p>本枚举值按 <b>GB/T 28181-2022 附录 J</b> 命名：
 * <ul>
 *   <li>200 = 中心服务器（系统）</li>
 *   <li>111 = DVR</li>
 *   <li>118 = NVR</li>
 *   <li>132 = 摄像机</li>
 *   <li>215 = 业务分组（2022）/ 虚拟组织目录（2016）</li>
 *   <li>216 = 虚拟组织（2022）/ 中心信令控制服务器（2016）</li>
 * </ul>
 *
 * <p>2016 与 2022 在 215/216 上语义冲突。需要按对端版本解释代码时，优先使用
 * {@link #fromCode(int, DirectoryVersion)}；不指定版本时退化为 2022（默认）。
 *
 * <p>历史命名常量 {@link #VIRTUAL_ORGANIZATION_DIRECTORY} 与
 * {@link #CENTER_SIGNAL_CONTROL_SERVER} 仅保留向后兼容；新代码应直接使用
 * {@link #BUSINESS_GROUP} / {@link #VIRTUAL_ORGANIZATION}。
 *
 * @author luna
 */
public enum DeviceGbType {
    CENTER_SERVER(200, "中心服务器"),
    DVR(111, "DVR"),
    NVR(118, "NVR"),
    CAMERA(132, "摄像机"),

    // ------------- GBT-28181-2022 标准命名（v1.7.x 起首选） -------------
    /** GB/T 28181-2022：215 业务分组。 */
    BUSINESS_GROUP(215, "业务分组"),
    /** GB/T 28181-2022：216 虚拟组织。 */
    VIRTUAL_ORGANIZATION(216, "虚拟组织"),

    // ------------- GBT-28181-2016 兼容别名（已废弃，按需保留） -------------
    /**
     * @deprecated GB/T 28181-2016 命名。在 2022 标准下 215=业务分组。
     *             新代码请直接使用 {@link #BUSINESS_GROUP} 并配合
     *             {@link #fromCode(int, DirectoryVersion)} 显式指定版本。
     */
    @Deprecated
    VIRTUAL_ORGANIZATION_DIRECTORY(215, "虚拟组织目录(2016 兼容)"),
    /**
     * @deprecated GB/T 28181-2016 命名。在 2022 标准下 216=虚拟组织。
     *             新代码请使用 {@link #VIRTUAL_ORGANIZATION}。
     */
    @Deprecated
    CENTER_SIGNAL_CONTROL_SERVER(216, "中心信令控制服务器(2016 兼容)");

    /** 2022 默认映射：code → enum；215=BUSINESS_GROUP, 216=VIRTUAL_ORGANIZATION。 */
    private static final Map<Integer, DeviceGbType> CODE_TO_TYPE_MAP_2022 = new HashMap<>();
    /** 2016 映射：215=VIRTUAL_ORGANIZATION_DIRECTORY, 216=CENTER_SIGNAL_CONTROL_SERVER。 */
    private static final Map<Integer, DeviceGbType> CODE_TO_TYPE_MAP_2016 = new HashMap<>();

    static {
        // 共享类型
        CODE_TO_TYPE_MAP_2022.put(CENTER_SERVER.code, CENTER_SERVER);
        CODE_TO_TYPE_MAP_2022.put(DVR.code, DVR);
        CODE_TO_TYPE_MAP_2022.put(NVR.code, NVR);
        CODE_TO_TYPE_MAP_2022.put(CAMERA.code, CAMERA);
        // 2022：215/216 走新命名
        CODE_TO_TYPE_MAP_2022.put(BUSINESS_GROUP.code, BUSINESS_GROUP);
        CODE_TO_TYPE_MAP_2022.put(VIRTUAL_ORGANIZATION.code, VIRTUAL_ORGANIZATION);

        CODE_TO_TYPE_MAP_2016.putAll(CODE_TO_TYPE_MAP_2022);
        // 2016：215/216 改写为旧命名
        CODE_TO_TYPE_MAP_2016.put(VIRTUAL_ORGANIZATION_DIRECTORY.code, VIRTUAL_ORGANIZATION_DIRECTORY);
        CODE_TO_TYPE_MAP_2016.put(CENTER_SIGNAL_CONTROL_SERVER.code, CENTER_SIGNAL_CONTROL_SERVER);
    }

    private final int    code;
    private final String description;

    DeviceGbType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 按 GBT-28181-2022 默认语义解析代码。
     * 215 → BUSINESS_GROUP，216 → VIRTUAL_ORGANIZATION。
     */
    public static DeviceGbType fromCode(int code) {
        return CODE_TO_TYPE_MAP_2022.get(code);
    }

    /**
     * 按指定版本解析代码（互通时显式指定对端版本）。
     *
     * @param code    类型代码
     * @param version 版本枚举，null 时退化为 2022
     */
    public static DeviceGbType fromCode(int code, DirectoryVersion version) {
        if (version == DirectoryVersion.V_2016) {
            return CODE_TO_TYPE_MAP_2016.get(code);
        }
        return CODE_TO_TYPE_MAP_2022.get(code);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
