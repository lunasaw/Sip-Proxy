package io.github.lunasaw.sip.common.sdp;

import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import org.springframework.stereotype.Component;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import java.nio.charset.Charset;

/**
 * IETF RFC 4566 标准 SDP 解析器。
 * <p>
 * 不感知任何协议方言；遇到非标准字段会抛 {@link SdpParseException}。
 * 方言层应在调用此 parser 前剥离非标准行。
 *
 * @author luna
 * @since 1.6.0
 */
@Component
public final class Rfc4566SdpParser implements SdpParser<SdpSessionDescription> {

    private static final SdpFactory SDP_FACTORY = SdpFactory.getInstance();

    @Override
    public SdpSessionDescription parse(String body, Charset charset) throws SdpParseException {
        SessionDescription sd = SDP_FACTORY.createSessionDescription(body);
        return SdpSessionDescription.getInstance(sd);
    }
}
