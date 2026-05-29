package io.github.lunasaw.gbproxy.test.sipgateway;

import io.github.lunasaw.gbproxy.test.TestApplication;
import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.sipgateway.core.notifier.NoopBusinessNotifier;
import io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayProperties;
import io.github.lunasaw.sipgateway.gb28181.forwarder.Gb28181EventForwarder;
import io.github.lunasaw.sipgateway.gb28181.handler.Gb28181Module;
import io.github.lunasaw.sipgateway.gb28181.store.InMemoryInviteContextStore;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：在完整 Spring 上下文下验证 sip-gateway-spring-boot-starter 自动装配生效。
 *
 * <ul>
 *   <li>GatewayProperties / Gb28181GatewayProperties 绑定（默认值）</li>
 *   <li>InviteContextStore 默认走内存实现</li>
 *   <li>BusinessNotifier 默认走 Noop 实现</li>
 *   <li>Gb28181EventForwarder + CommandHandlerRegistry + Gb28181Module 注入成功</li>
 *   <li>所有 Gb28181 命令 type 已注册到 Registry</li>
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class GatewayContextLoadTest {

    @Autowired
    private GatewayProperties coreProperties;

    @Autowired
    private Gb28181GatewayProperties gb28181Properties;

    @Autowired
    private InviteContextStore inviteContextStore;

    @Autowired
    private BusinessNotifier businessNotifier;

    @Autowired
    private Gb28181EventForwarder forwarder;

    @Autowired
    private Gb28181Module gb28181Module;

    @Autowired
    private CommandHandlerRegistry registry;

    @Test
    void contextLoadsWithGatewayBeans() {
        assertThat(coreProperties).isNotNull();
        assertThat(coreProperties.getNodeId()).isNotBlank();
        assertThat(gb28181Properties).isNotNull();
        assertThat(gb28181Properties.getInviteContextTtlMs()).isPositive();
        assertThat(inviteContextStore).isInstanceOf(InMemoryInviteContextStore.class);
        assertThat(businessNotifier).isInstanceOf(NoopBusinessNotifier.class);
        assertThat(forwarder).isNotNull();
        assertThat(gb28181Module).isNotNull();
        assertThat(gb28181Module.protocol()).isEqualTo("gb28181");
    }

    @Test
    void registryContainsGb28181Types() {
        // 抽样验证 Registry 已加载 GB28181 命令表
        assertThat(registry.getKnownProtocols()).contains("gb28181");
        // 抽样几个核心 type
        assertThat(registry.require("gb28181.Query.Catalog")).isNotNull();
        assertThat(registry.require("gb28181.Invite.Bye")).isNotNull();
        // 注解白名单
        assertThat(registry.require("gb28181.Control.Ptz")).isNotNull();
        assertThat(registry.require("gb28181.Invite.Play")).isNotNull();
    }
}
