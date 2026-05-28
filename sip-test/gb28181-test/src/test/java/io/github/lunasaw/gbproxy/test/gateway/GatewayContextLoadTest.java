package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
import io.github.lunasaw.gbproxy.gateway.forwarder.SipEventForwarder;
import io.github.lunasaw.gbproxy.gateway.notifier.NoopBusinessNotifier;
import io.github.lunasaw.gbproxy.gateway.store.InMemoryInviteContextStore;
import io.github.lunasaw.gbproxy.gateway.web.SipCommandController;
import io.github.lunasaw.gbproxy.test.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：在完整 Spring 上下文下验证 gb28181-gateway 自动装配生效。
 *
 * <ul>
 *   <li>GatewayProperties 绑定（默认值）</li>
 *   <li>InviteContextStore 默认走内存实现</li>
 *   <li>BusinessNotifier 默认走 Noop 实现</li>
 *   <li>SipEventForwarder + SipCommandController 注入成功</li>
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
