package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import org.springframework.stereotype.Component;

/**
 * GB/T 28181-2022 附录 G "f 字段" 解析器：把 {@code f=<媒体参数>} 写入
 * {@link GbSessionDescription#setMediaParam}。
 * <p>
 * 阶段一只取整行字符串，不解析内部 9 段结构（v/编码/分辨率/帧率/码率类型/码率大小 a/编码/码率/采样率）。
 * 内部结构化解析作为阶段二独立 parser，不修改本类。
 *
 * @author luna
 * @since 1.6.0
 */
@Component
public class FFieldParser implements GbSdpExtensionParser {

    private static final String PREFIX = "f=";

    @Override
    public boolean accepts(String line) {
        return line != null && line.startsWith(PREFIX);
    }

    @Override
    public void apply(String line, GbSessionDescription target) {
        target.setMediaParam(line.substring(PREFIX.length()));
    }

    @Override
    public boolean stripBeforeBaseParse() {
        return true;
    }
}
