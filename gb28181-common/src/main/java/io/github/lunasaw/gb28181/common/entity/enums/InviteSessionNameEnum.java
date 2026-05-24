package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * @author luna
 * @date 2023/10/13
 */
public enum InviteSessionNameEnum {

    /**
     *
     */
    PLAY("Play", "点播"),
    PLAY_BACK("PlayBack", "回放"),
    TALK("Talk", "语音对讲"),
    DOWNLOAD("Download", "下载"),


    ;

    private final String type;
    private final String desc;

    InviteSessionNameEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static boolean isValid(String sort) {
        return PLAY.getType().equals(sort) || PLAY_BACK.getType().equals(sort)
                || TALK.getType().equals(sort) || DOWNLOAD.getType().equals(sort);
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
