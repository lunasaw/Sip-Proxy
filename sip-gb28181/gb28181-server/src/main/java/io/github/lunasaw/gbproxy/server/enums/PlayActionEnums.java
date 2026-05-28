package io.github.lunasaw.gbproxy.server.enums;

import io.github.lunasaw.gbproxy.server.entity.InviteEntity;
import lombok.Getter;
import org.springframework.util.Assert;

/**
 * 国标点播操作类型
 *
 * @author luna
 */
@Getter
public enum PlayActionEnums {
    PLAY_RESUME("playResume", "回放暂停", null),
    PLAY_RANGE("playRange", "回放Seek", ""),
    PLAY_SPEED("playSpeed", "倍速回放", 1.0),
    PLAY_NOW("playNow", "继续回放", null);

    /** 操作类型标识 */
    private final String type;
    /** 操作描述 */
    private final String desc;
    /** 操作默认数据（如倍速值、定位时间等） */
    private final Object data;


    PlayActionEnums(String type, String desc, Object data) {
        this.type = type;
        this.desc = desc;
        this.data = data;
    }

    /**
     * 使用枚举自带的默认数据生成回放控制消息体。
     *
     * @return RTSP 控制消息字符串，不支持的操作返回 null
     */
    public String getControlBody() {
        return getControlBody(data);
    }

    /**
     * 使用指定数据生成回放控制消息体。
     *
     * @param data 操作数据（PLAY_RANGE 传 Long 类型秒数，PLAY_SPEED 传 Double 类型倍速）
     * @return RTSP 控制消息字符串，不支持的操作返回 null
     */
    public String getControlBody(Object data) {
        if (PLAY_RESUME.equals(this)) {
            return InviteEntity.playPause();
        } else if (PLAY_RANGE.equals(this)) {
            Assert.notNull(data, "回放Seek时间不能为空");
            return InviteEntity.playRange((Long) data);
        } else if (PLAY_SPEED.equals(this)) {
            Assert.notNull(data, "倍速回放倍数不能为空");
            return InviteEntity.playSpeed((Double) data);
        } else if (PLAY_NOW.equals(this)) {
            return InviteEntity.playNow();
        }
        return null;
    }
}