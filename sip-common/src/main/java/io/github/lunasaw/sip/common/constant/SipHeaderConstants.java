package io.github.lunasaw.sip.common.constant;

/**
 * GB/T 28181 SIP 扩展头域常量集中。
 *
 * <p>放在 sip-common 协议层是有意为之：头名是协议字面量（RFC 字符串），
 * 业务语义解析才放到 gb-{client,server}。这样既不破坏协议解耦边界，
 * 也方便测试用例统一断言。
 *
 * @author luna
 */
public final class SipHeaderConstants {

    private SipHeaderConstants() {}

    // ---------------- GB/T 28181-2022 §附录 I 协议版本标识 ----------------

    /**
     * 附录 I：协议版本扩展头域名。出现在 REGISTER 请求及其成功/失败响应中。
     * 例：{@code X-GB-Ver: 3.0}
     */
    public static final String X_GB_VER_HEADER = "X-GB-Ver";

    /** 附录 I 表 I.1：GB/T 28181-2011。 */
    public static final String X_GB_VER_2011   = "1.0";
    /** 附录 I 表 I.1：GB/T 28181-2011 修改补充文件。 */
    public static final String X_GB_VER_2011_1 = "1.1";
    /** 附录 I 表 I.1：GB/T 28181-2016。 */
    public static final String X_GB_VER_2016   = "2.0";
    /** 附录 I 表 I.1：GB/T 28181-2022。本框架默认携带值。 */
    public static final String X_GB_VER_2022   = "3.0";

    // ---------------- GB/T 28181-2022 §8.3 SIP 信令认证 -------------------

    /**
     * §8.3：Note 头域，承载摘要信息。
     * 形如 {@code Note: Digest nonce="<base64>", algorithm=SM3}
     */
    public static final String NOTE_HEADER                 = "Note";

    /**
     * §8.3：跨域转发时由信令安全路由网关注入的用户身份链头域。
     * 形如 {@code Monitor-User-Identity: gw1-user001-attr1}
     */
    public static final String MONITOR_USER_IDENTITY_HEADER = "Monitor-User-Identity";

    /** §8.3：分隔符为 "-"，用于网关 ID/用户 ID/身份属性串接。 */
    public static final String MONITOR_USER_IDENTITY_DELIMITER = "-";
}
