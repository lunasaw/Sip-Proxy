package io.github.lunasaw.gbproxy.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gb28181")
public class TestDeviceProperties {
    private ServerConfig server;
    private ClientConfig client;

    @Data
    public static class ServerConfig {
        private DeviceConfig device;
        private AuthConfig auth;
    }

    @Data
    public static class ClientConfig {
        private DeviceConfig device;
        private AuthConfig auth;
    }

    @Data
    public static class DeviceConfig {
        private String domain;
        private String serverId;
        private String deviceId;
        private String deviceName;
        private String manufacturer;
        private String model;
        private String firmware;
    }

    @Data
    public static class AuthConfig {
        private String username;
        private String password;
        private String realm;
    }
}