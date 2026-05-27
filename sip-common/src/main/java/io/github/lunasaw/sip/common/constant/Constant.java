package io.github.lunasaw.sip.common.constant;

/**
 * SIP公共常量定义。
 */
public class Constant {

    /** TCP传输协议标识。 */
    public static final String TCP = "TCP";

    /** UDP传输协议标识。 */
    public static final String UDP = "UDP";

    /** 默认User-Agent标识。 */
    public static final String AGENT = "sip-proxy";

    /** 认证密码头域名称。 */
    public static final String PASSWORD_HEADER = "AUTH_PASSWORD";

    /** UTF-8字符集名称。 */
    public static final String UTF_8 = "UTF-8";

    public static void main(String[] args) {
        System.out.println(2 << 0);
    }
}
