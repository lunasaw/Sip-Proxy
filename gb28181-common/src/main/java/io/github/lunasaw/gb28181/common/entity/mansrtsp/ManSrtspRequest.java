package io.github.lunasaw.gb28181.common.entity.mansrtsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181-2022 附录 B MANSRTSP 控制请求结构化模型。
 *
 * MANSRTSP 是 RTSP 风格的文本协议，平台通过 INFO 请求向设备下发回放控制命令。
 * 典型示例：
 * <pre>
 * PLAY MANSRTSP/1.0
 * CSeq: 1
 * Range: npt=now-
 *
 * PAUSE MANSRTSP/1.0
 * CSeq: 2
 * PauseTime: now
 *
 * PLAY MANSRTSP/1.0
 * CSeq: 3
 * Scale: 2.0
 *
 * TEARDOWN MANSRTSP/1.0
 * CSeq: 4
 * </pre>
 *
 * @author luna
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ManSrtspRequest {

    /**
     * 方法：PLAY / PAUSE / TEARDOWN
     */
    private String method;

    /**
     * 协议版本，默认 MANSRTSP/1.0
     */
    private String version;

    /**
     * 序列号
     */
    private Integer cSeq;

    /**
     * Range 头：npt=now- 或 npt=20.0-30.0 等
     */
    private String range;

    /**
     * Scale 头：倍速因子，1.0=正常播放，2.0=两倍速等
     */
    private Double scale;

    /**
     * PauseTime：暂停时间点（PAUSE 才有），如 "now"
     */
    private String pauseTime;

    /**
     * 解析失败或未结构化时保留的原始报文
     */
    private String raw;
}
