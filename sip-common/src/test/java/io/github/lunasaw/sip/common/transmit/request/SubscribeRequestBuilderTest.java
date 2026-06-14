package io.github.lunasaw.sip.common.transmit.request;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sip.header.EventHeader;
import javax.sip.message.Request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;

/**
 * {@link SubscribeRequestBuilder} 的 Event 头域构建单元测试。
 *
 * <p>对应根因修复：GB/T 28181-2022 N.4.2 规定 SUBSCRIBE 的 Event 头域必须携带数字 id（格式 "Catalog; id=num"，见 RFC 6665）。
 * 旧实现在 {@code customizeRequest} 中只判断 eventType 非空便调用 {@code createEventHeader(eventType, eventId)}，
 * 当 eventId 为 null 时 JAIN-SIP {@code Event.setEventId(null)} 抛 NPE（目录订阅、移动位置订阅均命中）。
 * 修复后 eventId 为空时回退到自动生成数字 id 的重载，保证 Event 头始终存在且 id 合规。</p>
 *
 * @author luna
 */
class SubscribeRequestBuilderTest {

    private final SubscribeRequestBuilder builder = new SubscribeRequestBuilder();

    private FromDevice fromDevice() {
        return FromDevice.getInstance("34020000002000000001", "127.0.0.1", 5060);
    }

    private ToDevice toDevice() {
        return ToDevice.getInstance("34020000001320000001", "127.0.0.1", 5061);
    }

    private static final String CATALOG_BODY = "<?xml version=\"1.0\"?><Query><CmdType>Catalog</CmdType>"
        + "<SN>1</SN><DeviceID>34020000001320000001</DeviceID></Query>";

    @Test
    @DisplayName("eventId 为 null 时仍构建 Event 头且 id 自动补为数字（根因兜底，对应目录/移动位置订阅）")
    void buildSubscribe_withNullEventId_fillsNumericEventId() {
        ToDevice to = toDevice();
        to.setEventType("Catalog");
        to.setEventId(null);

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType("Catalog");
        subscribeInfo.setExpires(3600);

        Request request = builder.buildSubscribeRequest(fromDevice(), to, CATALOG_BODY, subscribeInfo, "test-call-id");

        EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
        assertThat(eventHeader).as("eventId 为 null 也必须有 Event 头").isNotNull();
        assertThat(eventHeader.getEventType()).isEqualTo("Catalog");
        assertThat(eventHeader.getEventId())
            .as("N.4.2 要求 Event 头域携带数字 id")
            .isNotNull()
            .matches("\\d+");
    }

    @Test
    @DisplayName("eventId 显式指定时按原值构建 Event 头（保持既有 alarm/ptz 行为）")
    void buildSubscribe_withExplicitEventId_keepsValue() {
        ToDevice to = toDevice();
        to.setEventType("Alarm");
        to.setEventId("123456");

        SubscribeInfo subscribeInfo = new SubscribeInfo();
        subscribeInfo.setEventType("Alarm");
        subscribeInfo.setEventId("123456");
        subscribeInfo.setExpires(3600);

        Request request = builder.buildSubscribeRequest(fromDevice(), to, CATALOG_BODY, subscribeInfo, "test-call-id");

        EventHeader eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
        assertThat(eventHeader).isNotNull();
        assertThat(eventHeader.getEventType()).isEqualTo("Alarm");
        assertThat(eventHeader.getEventId()).isEqualTo("123456");
    }
}
