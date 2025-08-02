package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.gbproxy.server.config.SipServerProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author weidian
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sip")
public class TestDeviceProperties {

    private SipServerProperties server;
    private SipClientProperties client;

}