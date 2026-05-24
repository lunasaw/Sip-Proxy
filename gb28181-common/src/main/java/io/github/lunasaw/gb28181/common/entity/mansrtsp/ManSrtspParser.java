package io.github.lunasaw.gb28181.common.entity.mansrtsp;

import org.apache.commons.lang3.StringUtils;

/**
 * GB28181-2022 附录 B MANSRTSP 文本协议解析。
 *
 * @author luna
 */
public final class ManSrtspParser {

    private ManSrtspParser() {
    }

    /**
     * 解析 MANSRTSP 请求文本到结构化 {@link ManSrtspRequest}。
     * 解析失败时返回 raw 字段带原始内容、其他字段为 null 的对象。
     */
    public static ManSrtspRequest parse(String text) {
        ManSrtspRequest.ManSrtspRequestBuilder builder = ManSrtspRequest.builder().raw(text);
        if (StringUtils.isBlank(text)) {
            return builder.build();
        }

        String[] lines = text.replace("\r\n", "\n").split("\n");
        if (lines.length == 0) {
            return builder.build();
        }

        // 首行：METHOD MANSRTSP/1.0
        String[] firstLineParts = lines[0].trim().split("\\s+");
        if (firstLineParts.length >= 1) {
            builder.method(firstLineParts[0]);
        }
        if (firstLineParts.length >= 2) {
            builder.version(firstLineParts[1]);
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            switch (name) {
                case "CSeq":
                    try {
                        builder.cSeq(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "Range":
                    builder.range(value);
                    break;
                case "Scale":
                    try {
                        builder.scale(Double.parseDouble(value));
                    } catch (NumberFormatException ignored) {
                    }
                    break;
                case "PauseTime":
                    builder.pauseTime(value);
                    break;
                default:
                    // 忽略未识别的头
            }
        }
        return builder.build();
    }
}
