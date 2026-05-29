package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;

/**
 * GB28181 SDP 扩展字段解析策略。
 * <p>
 * 每个实现负责一类非 RFC 4566 标准字段：
 * <ul>
 *   <li>顶级 GB 扩展行（{@code y=}、{@code f=}）— {@link #stripBeforeBaseParse()} 返回 {@code true}</li>
 *   <li>GB 私有 {@code a=} 属性（{@code a=streamprofile}、{@code a=downloadspeed} 等）—
 *       {@link #stripBeforeBaseParse()} 返回 {@code false}（已是 RFC 4566 合法字段）</li>
 * </ul>
 *
 * <p>由 {@link Gb28181SdpParser} 统一编排：先剥需剥行交 base parser，再对每行 {@link #accepts(String)}
 * 测试，命中即写回模型。
 *
 * @author luna
 * @since 1.6.0
 */
public interface GbSdpExtensionParser {

    /**
     * 判断本 parser 是否处理该行。
     */
    boolean accepts(String line);

    /**
     * 解析单行扩展属性，结果写入 {@code target}。
     * <p>
     * 单条解析失败应抛 {@link RuntimeException} 由 {@link Gb28181SdpParser} 捕获记 WARN，
     * 不影响其他字段；不能让整个 SDP 解析因单字段错误而失败。
     */
    void apply(String line, GbSessionDescription target);

    /**
     * 是否需要在调用 RFC 4566 base parser 前剥离该行。
     * <p>
     * 顶级行（{@code y=}、{@code f=}）必须剥离，否则 JAIN-SDP 抛 {@code SdpParseException}；
     * {@code a=} 属性已是 RFC 4566 合法字段，无需剥离。
     */
    boolean stripBeforeBaseParse();
}
