package io.github.lunasaw.sip.common.enums;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.header.ContentTypeHeader;

import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.SneakyThrows;

/**
 * SIP消息体内容类型枚举，缓存对应的 ContentTypeHeader 实例。
 */
public enum ContentTypeEnum {

    /**
     * xml
     */
    APPLICATION_XML("Application", "MANSCDP+xml"),

    /**
     * sdp
     */
    APPLICATION_SDP("APPLICATION", "SDP"),
    /**
     *
     */
    APPLICATION_MAN_SRTSP("Application", "MANSRTSP"),

    ;

    private static final Map<String, ContentTypeHeader> MAP = new ConcurrentHashMap<>();
    private final String                                type;
    private final String                                subtype;

    ContentTypeEnum(String type, String subtype) {
        this.type = type;
        this.subtype = subtype;
    }

    /**
     * 根据 ContentTypeHeader 查找对应枚举值。
     *
     * @param header ContentTypeHeader
     * @return 匹配的枚举值，不存在时返回null
     */
    public static ContentTypeEnum fromContentTypeHeader(ContentTypeHeader header) {
        for (ContentTypeEnum contentType : values()) {
            if (contentType.type.equals(header.getContentType())
                && contentType.subtype.equals(header.getContentSubType())) {
                return contentType;
            }
        }
        return null;
    }

    /**
     * 根据字符串查找对应枚举值（忽略大小写）。
     *
     * @param contentType 内容类型字符串，格式为 type/subtype
     * @return 匹配的枚举值，不存在时返回null
     */
    public static ContentTypeEnum fromString(String contentType) {
        for (ContentTypeEnum contentTypeEnum : values()) {
            if (contentTypeEnum.toString().equalsIgnoreCase(contentType)) {
                return contentTypeEnum;
            }
        }
        return null;
    }

    /**
     * 获取对应的 ContentTypeHeader 实例（带缓存）。
     *
     * @return ContentTypeHeader实例
     */
    @SneakyThrows
    public ContentTypeHeader getContentTypeHeader() {
        String key = toString();
        if (MAP.containsKey(key)) {
            return MAP.get(key);
        } else {
            ContentTypeHeader contentTypeHeader = SipRequestUtils.createContentTypeHeader(type, subtype);
            MAP.put(key, contentTypeHeader);
            return contentTypeHeader;
        }
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
    }
}