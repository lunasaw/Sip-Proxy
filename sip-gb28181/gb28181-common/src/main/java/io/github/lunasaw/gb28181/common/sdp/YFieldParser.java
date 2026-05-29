package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import org.springframework.stereotype.Component;

/**
 * GB/T 28181-2022 附录 G "y 字段" 解析器：把 {@code y=<SSRC>} 写入
 * {@link GbSessionDescription#setSsrc}。
 *
 * @author luna
 * @since 1.6.0
 */
@Component
public class YFieldParser implements GbSdpExtensionParser {

    private static final String PREFIX = "y=";

    @Override
    public boolean accepts(String line) {
        return line != null && line.startsWith(PREFIX);
    }

    @Override
    public void apply(String line, GbSessionDescription target) {
        target.setSsrc(line.substring(PREFIX.length()));
    }

    @Override
    public boolean stripBeforeBaseParse() {
        return true;
    }
}
