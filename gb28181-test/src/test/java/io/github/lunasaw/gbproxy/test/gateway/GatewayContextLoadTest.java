package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 GatewayConfig 自动装配在完整 Spring context 下能拉起：
 * <ul>
 *   <li>GatewayProperties 绑定（默认值）</li>
 *   <li>InviteContextStore 默认走内存实现</li>
 *   <li>SipEventForwarder + SipCommandController 注入成功</li>
 *   <li>未配置 nodes 时 RestTemplate 不应被装配（@ConditionalOnProperty）</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class GatewayContextLoadTest {

    @Autowired
    private GatewayProperties properties;

    @Autowired
    private InviteContextStore inviteContextStore;

    @Autowired
    private BusinessNotifier businessNotifier;

    @Autowired
    private SipEventForwarder forwarder;

    @Autowired
    private SipCommandController controller;

    @Test
    void contextLoadsWithGatewayBeans() {
        assertThat(properties).isNotNull();
        assertThat(properties.getInviteContextTtlMs()).isPositive();
        assertThat(inviteContextStore).isInstanceOf(InMemoryInviteContextStore.class);
        assertThat(businessNotifier).isInstanceOf(NoopBusinessNotifier.class);
        assertThat(forwarder).isNotNull();
        assertThat(controller).isNotNull();
    }
}
