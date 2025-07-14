package io.github.lunasaw.sip.common.transmit;

import com.luna.common.os.SystemInfoUtil;
import gov.nist.javax.sip.SipProviderImpl;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategy;
import io.github.lunasaw.sip.common.transmit.strategy.SipRequestStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sip.SipListener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SipSender重构后的测试类
 * 验证新API的功能和兼容性
 *
 * @author lin
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class SipSenderRefactorTest {

    @Mock
    private FromDevice fromDevice;

    @Mock
    private ToDevice toDevice;

    @Mock
    private Event errorEvent;

    @Mock
    private Event okEvent;

    @Mock
    private SipProviderImpl sipProvider;

    @Mock
    private javax.sip.header.CallIdHeader callIdHeader;

    private SipLayer sipLayer;

    @BeforeEach
    void setUp() {
        // 创建SipLayer实例，使用mock的SipListener
        sipLayer = new SipLayer();
        fromDevice = FromDevice.getInstance("34020000001320000001", "127.0.0.1", 5060);
        toDevice = ToDevice.getInstance("34020000001320000002", "127.0.0.1", 5060);

        // 初始化监听点，确保测试环境正确设置
        sipLayer.setSipListener(CustomerSipListener.getInstance());
        sipLayer.addListeningPoint("127.0.0.1", 5060);
    }

    @Test
    void testRequestBuilder() {
        // 使用MockedStatic来mock静态方法
        try (MockedStatic<SipLayer> sipLayerMock = mockStatic(SipLayer.class)) {
            // Mock静态方法 - 确保在构造函数调用之前生效
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider("127.0.0.1")).thenReturn(sipProvider);
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider()).thenReturn(sipProvider);
            when(sipProvider.getNewCallId()).thenReturn(callIdHeader);

            // 测试MESSAGE请求建造者
            SipSender.SipRequestBuilder messageBuilder = SipSender.request(fromDevice, toDevice, "MESSAGE");
            assertNotNull(messageBuilder);

            // 测试建造者链式调用
            messageBuilder.content("Hello World")
                    .errorEvent(errorEvent)
                    .okEvent(okEvent);

            // 验证建造者设置
            assertNotNull(messageBuilder);
        }
    }

    @Test
    void testStrategyFactory() {
        // 测试获取策略
        SipRequestStrategy messageStrategy = SipRequestStrategyFactory.getStrategy("MESSAGE");
        assertNotNull(messageStrategy);

        SipRequestStrategy inviteStrategy = SipRequestStrategyFactory.getStrategy("INVITE");
        assertNotNull(inviteStrategy);

        SipRequestStrategy subscribeStrategy = SipRequestStrategyFactory.getStrategy("SUBSCRIBE");
        assertNotNull(subscribeStrategy);

        SipRequestStrategy notifyStrategy = SipRequestStrategyFactory.getStrategy("NOTIFY");
        assertNotNull(notifyStrategy);

        SipRequestStrategy byeStrategy = SipRequestStrategyFactory.getStrategy("BYE");
        assertNotNull(byeStrategy);

        SipRequestStrategy ackStrategy = SipRequestStrategyFactory.getStrategy("ACK");
        assertNotNull(ackStrategy);

        SipRequestStrategy infoStrategy = SipRequestStrategyFactory.getStrategy("INFO");
        assertNotNull(infoStrategy);
    }

    @Test
    void testRegisterStrategy() {
        // 测试注册策略
        SipRequestStrategy registerStrategy = SipRequestStrategyFactory.getRegisterStrategy(3600);
        assertNotNull(registerStrategy);

        SipRequestStrategy registerStrategy2 = SipRequestStrategyFactory.getRegisterStrategy(7200);
        assertNotNull(registerStrategy2);
    }

    @Test
    void testCustomStrategyRegistration() {
        // 测试自定义策略注册
        SipRequestStrategy customStrategy = mock(SipRequestStrategy.class);

        // 注册自定义策略
        SipRequestStrategyFactory.registerStrategy("CUSTOM", customStrategy);

        // 获取自定义策略
        SipRequestStrategy retrievedStrategy = SipRequestStrategyFactory.getStrategy("CUSTOM");
        assertNotNull(retrievedStrategy);
        assertEquals(customStrategy, retrievedStrategy);

        // 移除自定义策略
        SipRequestStrategyFactory.removeStrategy("CUSTOM");

        // 验证策略已被移除
        SipRequestStrategy removedStrategy = SipRequestStrategyFactory.getStrategy("CUSTOM");
        assertNull(removedStrategy);
    }

    @Test
    void testUnsupportedMethod() {
        // 测试不支持的方法
        SipRequestStrategy unsupportedStrategy = SipRequestStrategyFactory.getStrategy("UNSUPPORTED");
        assertNull(unsupportedStrategy);
    }

    @Test
    void testCompatibilityMethods() {
        // 测试兼容性方法的存在性
        // 这些方法应该存在但可能抛出异常（因为依赖外部组件）

        // 验证方法存在
        assertDoesNotThrow(() -> {
            // 这些方法调用可能会因为缺少依赖而失败，但我们只验证方法存在
            SipSender.class.getMethod("doMessageRequest", FromDevice.class, ToDevice.class, String.class);
            SipSender.class.getMethod("doInviteRequest", FromDevice.class, ToDevice.class, String.class, String.class);
            SipSender.class.getMethod("doSubscribeRequest", FromDevice.class, ToDevice.class, String.class, SubscribeInfo.class);
            SipSender.class.getMethod("doRegisterRequest", FromDevice.class, ToDevice.class, Integer.class);
        });
    }

    @Test
    void testBuilderChaining() {
        // 使用MockedStatic来mock静态方法
        try (MockedStatic<SipLayer> sipLayerMock = mockStatic(SipLayer.class)) {
            // Mock静态方法 - 确保在构造函数调用之前生效
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider("127.0.0.1")).thenReturn(sipProvider);
            sipLayerMock.when(SipLayer::getUdpSipProvider).thenReturn(sipProvider);
            when(sipProvider.getNewCallId()).thenReturn(callIdHeader);

            // 测试建造者链式调用
            SipSender.SipRequestBuilder builder = SipSender.request(fromDevice, toDevice, "MESSAGE")
                    .content("Test Content")
                    .errorEvent(errorEvent)
                    .okEvent(okEvent)
                    .callId("test-call-id");

            assertNotNull(builder);

            // 验证链式调用返回的是同一个对象
            SipSender.SipRequestBuilder chainedBuilder = builder
                    .content("New Content")
                    .errorEvent(null);

            assertEquals(builder, chainedBuilder);
        }
    }

    @Test
    void testStrategyFactorySingleton() {
        // 测试策略工厂的单例特性
        SipRequestStrategy strategy1 = SipRequestStrategyFactory.getStrategy("MESSAGE");
        SipRequestStrategy strategy2 = SipRequestStrategyFactory.getStrategy("MESSAGE");

        // 应该返回相同的策略实例
        assertEquals(strategy1, strategy2);
    }

    @Test
    void testBuilderWithDifferentMethods() {
        // 使用MockedStatic来mock静态方法
        try (MockedStatic<SipLayer> sipLayerMock = mockStatic(SipLayer.class)) {
            // Mock静态方法 - 确保在构造函数调用之前生效
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider("127.0.0.1")).thenReturn(sipProvider);
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider()).thenReturn(sipProvider);
            when(sipProvider.getNewCallId()).thenReturn(callIdHeader);

            // 测试不同方法的建造者
            // 测试MESSAGE方法
            SipSender.SipRequestBuilder messageBuilder = SipSender.request(fromDevice, toDevice, "MESSAGE");
            assertNotNull(messageBuilder);

            // 测试INVITE方法
            SipSender.SipRequestBuilder inviteBuilder = SipSender.request(fromDevice, toDevice, "INVITE");
            assertNotNull(inviteBuilder);

            // 测试SUBSCRIBE方法
            SipSender.SipRequestBuilder subscribeBuilder = SipSender.request(fromDevice, toDevice, "SUBSCRIBE");
            assertNotNull(subscribeBuilder);

            // 测试NOTIFY方法
            SipSender.SipRequestBuilder notifyBuilder = SipSender.request(fromDevice, toDevice, "NOTIFY");
            assertNotNull(notifyBuilder);

            // 测试BYE方法
            SipSender.SipRequestBuilder byeBuilder = SipSender.request(fromDevice, toDevice, "BYE");
            assertNotNull(byeBuilder);

            // 测试ACK方法
            SipSender.SipRequestBuilder ackBuilder = SipSender.request(fromDevice, toDevice, "ACK");
            assertNotNull(ackBuilder);

            // 测试INFO方法
            SipSender.SipRequestBuilder infoBuilder = SipSender.request(fromDevice, toDevice, "INFO");
            assertNotNull(infoBuilder);
        }
    }

    @Test
    void testBuilderWithSubscribeInfo() {
        // 使用MockedStatic来mock静态方法
        try (MockedStatic<SipLayer> sipLayerMock = mockStatic(SipLayer.class)) {
            // Mock静态方法 - 确保在构造函数调用之前生效
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider("127.0.0.1")).thenReturn(sipProvider);
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider()).thenReturn(sipProvider);
            when(sipProvider.getNewCallId()).thenReturn(callIdHeader);

            // 测试带有SubscribeInfo的建造者
            SubscribeInfo subscribeInfo = mock(SubscribeInfo.class);

            SipSender.SipRequestBuilder builder = SipSender.request(fromDevice, toDevice, "SUBSCRIBE")
                    .subscribeInfo(subscribeInfo)
                    .expires(3600);

            assertNotNull(builder);
        }
    }

    @Test
    void testBuilderWithSubject() {
        // 使用MockedStatic来mock静态方法
        try (MockedStatic<SipLayer> sipLayerMock = mockStatic(SipLayer.class)) {
            // Mock静态方法 - 确保在构造函数调用之前生效
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider("127.0.0.1")).thenReturn(sipProvider);
            sipLayerMock.when(() -> SipLayer.getUdpSipProvider()).thenReturn(sipProvider);
            when(sipProvider.getNewCallId()).thenReturn(callIdHeader);

            // 测试带有Subject的建造者
            SipSender.SipRequestBuilder builder = SipSender.request(fromDevice, toDevice, "INVITE")
                    .subject("test-subject")
                    .content("test-content");

            assertNotNull(builder);
        }
    }

    @Test
    void testSipLayerConstructorInjection() {
        // 测试SipLayer的构造器注入
        assertNotNull(sipLayer);
        // 验证SipLayer实例已正确创建
        assertDoesNotThrow(() -> {
            sipLayer.addListeningPoint("127.0.0.1", 8117);
        });
    }
}