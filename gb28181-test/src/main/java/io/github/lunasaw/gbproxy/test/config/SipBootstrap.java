package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.gbproxy.server.config.SipServerProperties;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.sip.common.transmit.CustomerSipListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.sip.SipListener;

/**
 * SIP 启动引导。
 * <p>
 * 业务方接入框架时, SipLayer 默认不会自动绑定 {@link SipListener} 到自身, 也不会自动按
 * {@code sip.client.port} / {@code sip.server.port} 建立监听点。本类把这两件事
 * 收敛到一个 {@code @PostConstruct}, 业务方拷贝即可使用, 无需在每个测试类里手写
 * {@code sipLayer.setSipListener(...)} 与 {@code sipLayer.addListeningPoint(...)}。
 * <p>
 * 仅当 Spring 容器中存在 {@link SipLayer} 时生效, 因此即使业务方只引入 sip-common
 * 也能复用同一个引导逻辑。
 */
@Slf4j
@Configuration
@ConditionalOnBean(SipLayer.class)
@RequiredArgsConstructor
public class SipBootstrap {

    private final SipLayer sipLayer;
    private final ObjectProvider<SipServerProperties> serverProperties;
    private final ObjectProvider<SipClientProperties> clientProperties;

    @PostConstruct
    public void bootstrap() {
        sipLayer.setSipListener(CustomerSipListener.getInstance());

        SipServerProperties server = serverProperties.getIfAvailable();
        if (server != null && server.isEnabled()) {
            sipLayer.addListeningPoint(server.getIp(), server.getPort());
            log.info("SIP server listening point ready: {}:{}", server.getIp(), server.getPort());
        }

        SipClientProperties client = clientProperties.getIfAvailable();
        if (client != null && client.isEnabled()) {
            sipLayer.addListeningPoint(client.getDomain(), client.getPort());
            log.info("SIP client listening point ready: {}:{}", client.getDomain(), client.getPort());
        }
    }
}
