package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * @author luna
 * @date 2023/10/13
 */
public enum ManufacturerEnum {

    /**
     * 硬件厂家
     */
    TP_LINK("TP-LINK", "TP-LINK"),


    ;

    private final String type;
    private final String desc;

    ManufacturerEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }


    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
