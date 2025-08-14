package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceSubscribe;
import io.github.lunasaw.gbproxy.client.transmit.request.subscribe.SubscribeRequestHandler;
import io.github.lunasaw.gbproxy.client.transmit.response.register.RegisterProcessorHandler;
import io.github.lunasaw.gbproxy.server.transmit.request.notify.ServerNotifyProcessorHandler;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.subscribe.SubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;

/**
 * 测试专用配置类
 * 用于简化测试环境，禁用复杂的组件，并提供测试专用的Bean实现
 *
 * @author luna
 * @date 2025/01/23
 */
@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    // 排除可能导致问题的自动配置
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
})
@Slf4j
public class TestConfiguration {

    /**
     * 测试专用的RegisterProcessorHandler实现
     */
    @Bean
    @Primary
    public RegisterProcessorHandler testRegisterProcessorHandler() {
        return new RegisterProcessorHandler() {
            @Override
            public void registerSuccess(String toUserId) {
                log.info("🎉 Test RegisterProcessorHandler - 注册成功: toUserId={}", toUserId);
            }

            @Override
            public void handleUnauthorized(ResponseEvent evt, String toUserId, String callId) {
                log.info("🔐 Test RegisterProcessorHandler - 处理未授权: toUserId={}, callId={}", toUserId, callId);
            }

            @Override
            public void handleRegisterFailure(String toUserId, int statusCode) {
                log.info("❌ Test RegisterProcessorHandler - 注册失败: toUserId={}, statusCode={}", toUserId, statusCode);
            }
        };
    }

    /**
     * 测试专用的SubscribeRequestHandler实现
     */
    @Bean
    @Primary
    public SubscribeRequestHandler testSubscribeRequestHandler() {
        return new SubscribeRequestHandler() {
            @Override
            public void putSubscribe(String userId, SubscribeInfo subscribeInfo) {
                log.info("🔔 Test SubscribeRequestHandler - 添加订阅: userId={}, subscribeInfo={}", userId, subscribeInfo);
            }

            @Override
            public DeviceSubscribe getDeviceSubscribe(DeviceQuery deviceQuery) {
                log.info("📋 Test SubscribeRequestHandler - 获取设备订阅: deviceQuery={}", deviceQuery);
                return new DeviceSubscribe();
            }
        };
    }

    /**
     * 测试专用的ServerNotifyProcessorHandler实现
     */
    @Bean
    @Primary
    public ServerNotifyProcessorHandler testServerNotifyProcessorHandler() {
        return new ServerNotifyProcessorHandler() {
            @Override
            public void handleNotifyRequest(RequestEvent evt, FromDevice fromDevice) {
                log.info("📢 Test ServerNotifyProcessorHandler - 处理NOTIFY请求: fromDevice={}", fromDevice);
            }

            @Override
            public boolean validateDevicePermission(RequestEvent evt) {
                log.info("🔒 Test ServerNotifyProcessorHandler - 验证设备权限: 默认允许");
                return true;
            }

            @Override
            public void handleNotifyError(RequestEvent evt, String errorMessage) {
                log.info("⚠️ Test ServerNotifyProcessorHandler - 处理NOTIFY错误: {}", errorMessage);
            }

            @Override
            public void deviceNotifyUpdate(String userId, DeviceOtherUpdateNotify deviceOtherUpdateNotify) {
                log.info("🔄 Test ServerNotifyProcessorHandler - 设备更新通知: userId={}", userId);
            }
        };
    }
}