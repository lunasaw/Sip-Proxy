package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * @author luna
 * @date 2023/10/13
 */
public enum StreamModeEnum {

    /**
     * 数据流传输模式
     */
    UDP("UDP", "UDP"),
    TCP_ACTIVE("TCP-ACTIVE", "TCP主动"),
    TCP_PASSIVE("TCP-PASSIVE", "TCP被动"),

    ;

    private final String type;
    private final String desc;

    StreamModeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static boolean isValid(String sort) {
        return UDP.getType().equals(sort) || TCP_ACTIVE.getType().equals(sort) || TCP_PASSIVE.getType().equals(sort);
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
