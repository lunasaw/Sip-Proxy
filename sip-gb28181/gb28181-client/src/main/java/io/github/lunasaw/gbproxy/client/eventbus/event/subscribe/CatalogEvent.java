package io.github.lunasaw.gbproxy.client.eventbus.event.subscribe;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import lombok.Setter;

/**
 * 目录订阅变更事件，定义目录条目状态变更类型常量。
 */
@Getter
@Setter
public class CatalogEvent extends ApplicationEvent {
    /**
     * 构造目录事件。
     *
     * @param source 事件来源对象
     */
    public CatalogEvent(Object source) {
        super(source);
    }

    /**
     * 上线
     */
    public static final String ON         = "ON";

    /**
     * 离线
     */
    public static final String OFF        = "OFF";

    /**
     * 视频丢失
     */
    public static final String VIDEO_LOST = "VIDEO_LOST";

    /**
     * 故障
     */
    public static final String DEFECT     = "DEFECT";

    /**
     * 增加
     */
    public static final String ADD        = "ADD";

    /**
     * 删除
     */
    public static final String DEL        = "DEL";

    /**
     * 更新
     */
    public static final String UPDATE     = "UPDATE";

}
