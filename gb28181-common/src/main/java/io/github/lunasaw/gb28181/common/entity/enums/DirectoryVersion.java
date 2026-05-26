package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * GBT-28181 目录类型语义版本（附录 J）。
 *
 * <p>2016 与 2022 标准在 215/216 类型代码语义上**不一致**：
 * <ul>
 *   <li><b>V_2016</b>：215=虚拟组织目录，216=中心信令控制服务器</li>
 *   <li><b>V_2022</b>：215=业务分组，216=虚拟组织</li>
 * </ul>
 *
 * <p>由 {@code sip.common.directory-version} 配置控制全局缺省值，业务方在解析
 * {@link DeviceGbType} 时可以传入对端协商出的版本以避免互通错位。
 *
 * @author luna
 */
public enum DirectoryVersion {
    /** GB/T 28181-2016 标准。 */
    V_2016,
    /** GB/T 28181-2022 标准（默认）。 */
    V_2022;

    /**
     * 按字符串解析版本，缺省 V_2022。支持 {@code "2016"} / {@code "2022"} / {@code "v_2022"} 等。
     *
     * @param raw 配置串
     * @return 对应枚举，不可识别返回 V_2022
     */
    public static DirectoryVersion parse(String raw) {
        if (raw == null) {
            return V_2022;
        }
        String trimmed = raw.trim().toUpperCase();
        if (trimmed.contains("2016")) {
            return V_2016;
        }
        return V_2022;
    }
}
