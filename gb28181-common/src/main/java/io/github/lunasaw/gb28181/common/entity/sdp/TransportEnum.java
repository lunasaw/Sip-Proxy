package io.github.lunasaw.gb28181.common.entity.sdp;

/**
 * GB/T 28181-2022 附录 G "m 字段" 定义的传输方式。
 * <ul>
 *   <li>{@link #UDP} — {@code RTP/AVP}（默认）</li>
 *   <li>{@link #TCP} — {@code TCP/RTP/AVP}（IETF RFC 4571）</li>
 * </ul>
 *
 * @author luna
 * @since 1.6.0
 */
public enum TransportEnum {

    UDP("RTP/AVP"),
    TCP("TCP/RTP/AVP");

    private final String protoToken;

    TransportEnum(String protoToken) {
        this.protoToken = protoToken;
    }

    public String getProtoToken() {
        return protoToken;
    }

    public static TransportEnum fromProtoToken(String token) {
        if (token == null) {
            return null;
        }
        for (TransportEnum t : values()) {
            if (t.protoToken.equalsIgnoreCase(token)) {
                return t;
            }
        }
        return null;
    }
}
