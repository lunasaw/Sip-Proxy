package io.github.lunasaw.sip.common.sdp;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;

import javax.sdp.SdpParseException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * SDP 协议解析器抽象。
 * <p>
 * 实现按协议族划分：sip-common 提供 RFC 4566 base，
 * 各协议方言在自己的模块内提供专用实现并组合 base。
 *
 * @param <T> 解析输出模型
 * @author luna
 * @since 1.6.0
 */
public interface SdpParser<T extends SdpSessionDescription> {

    T parse(String body, Charset charset) throws SdpParseException;

    default T parse(String body) throws SdpParseException {
        return parse(body, StandardCharsets.UTF_8);
    }
}
