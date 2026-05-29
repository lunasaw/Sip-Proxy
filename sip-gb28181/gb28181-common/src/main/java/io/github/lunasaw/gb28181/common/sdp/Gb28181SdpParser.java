package io.github.lunasaw.gb28181.common.sdp;

import io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription;
import io.github.lunasaw.sip.common.entity.SdpSessionDescription;
import io.github.lunasaw.sip.common.sdp.Rfc4566SdpParser;
import io.github.lunasaw.sip.common.sdp.SdpParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sdp.SdpParseException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * GB/T 28181 SDP 解析器：RFC 4566 base + 附录 G 扩展字段链。
 *
 * <p>解析流程：
 * <ol>
 *   <li>按行扫描，将"需剥离"的扩展行（{@link GbSdpExtensionParser#stripBeforeBaseParse()} == true，
 *       例如 {@code y=}、{@code f=}）从原文剥离</li>
 *   <li>剩余行交 {@link Rfc4566SdpParser} 解析得到标准 SDP 模型</li>
 *   <li>对原文每一行（含被剥离的、含未被剥离的 {@code a=xxx}）逐个 {@link GbSdpExtensionParser#accepts}
 *       测试，命中即写入模型</li>
 *   <li>调 {@link Gb28181SemanticInterpreter} 把标准字段（{@code s=}/{@code m=}/{@code c=}）翻译成 GB 业务枚举</li>
 * </ol>
 *
 * <p>错误策略：单条扩展行解析失败 = WARN 日志 + 跳过；标准字段语法错误才抛 {@link SdpParseException}。
 *
 * @author luna
 * @since 1.6.0
 */
@Slf4j
@Component
public final class Gb28181SdpParser implements SdpParser<GbSessionDescription> {

    private final Rfc4566SdpParser baseParser;
    private final List<GbSdpExtensionParser> extensions;
    private final Gb28181SemanticInterpreter interpreter;

    public Gb28181SdpParser(Rfc4566SdpParser baseParser,
                            List<GbSdpExtensionParser> extensions,
                            Gb28181SemanticInterpreter interpreter) {
        this.baseParser = baseParser;
        this.extensions = extensions;
        this.interpreter = interpreter;
    }

    @Override
    public GbSessionDescription parse(String body, Charset charset) throws SdpParseException {
        SplitResult split = splitExtensionLines(body);
        SdpSessionDescription rfc = baseParser.parse(split.cleanedBody, charset);

        GbSessionDescription gb = new GbSessionDescription(rfc.getBaseSdb());
        for (String line : split.allOriginalLines) {
            applyOne(line, gb);
        }
        interpreter.interpret(gb);
        return gb;
    }

    private void applyOne(String line, GbSessionDescription target) {
        for (GbSdpExtensionParser ext : extensions) {
            if (ext.accepts(line)) {
                try {
                    ext.apply(line, target);
                } catch (Exception e) {
                    log.warn("GB SDP extension parse failed, skip line: {}", line, e);
                }
                return;
            }
        }
    }

    /**
     * 把原文按行拆出：base 解析需吃的"剩余行"、原文全部行（用于 strategy 写回）。
     * 剥离判定：行首匹配某个 {@link GbSdpExtensionParser#stripBeforeBaseParse() stripBefore=true} 的 parser。
     */
    private SplitResult splitExtensionLines(String body) {
        String[] lines = body.split("\\r?\\n");
        StringBuilder cleaned = new StringBuilder(body.length());
        List<String> all = new ArrayList<>(lines.length);
        for (String raw : lines) {
            String line = raw.trim();
            all.add(line);
            if (!shouldStrip(line)) {
                cleaned.append(line).append("\r\n");
            }
        }
        return new SplitResult(cleaned.toString(), all);
    }

    private boolean shouldStrip(String line) {
        for (GbSdpExtensionParser ext : extensions) {
            if (ext.stripBeforeBaseParse() && ext.accepts(line)) {
                return true;
            }
        }
        return false;
    }

    private static final class SplitResult {
        final String cleanedBody;
        final List<String> allOriginalLines;

        SplitResult(String cleanedBody, List<String> allOriginalLines) {
            this.cleanedBody = cleanedBody;
            this.allOriginalLines = allOriginalLines;
        }
    }
}
