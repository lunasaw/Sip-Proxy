package io.github.lunasaw.gbproxy.test.config;

import io.github.lunasaw.gbproxy.server.transmit.cmd.DeviceSessionCache;
import io.github.lunasaw.sip.common.entity.ToDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeviceSessionCache 示例实现：内存缓存。
 * 生产环境可替换为 Redis 或数据库实现。
 * 设备注册时调用 {@link #register} 写入，注销时调用 {@link #remove}。
 */
@Slf4j
@Component
public class SipBusinessConfig implements DeviceSessionCache {

    private final Map<String, ToDevice> cache = new ConcurrentHashMap<>();

    public void register(String deviceId, String ip, int port, String transport) {
        cache.put(deviceId, ToDevice.getInstance(deviceId, ip, port, transport));
        log.info("device registered: {} -> {}:{} ({})", deviceId, ip, port, transport);
    }

    public void remove(String deviceId) {
        cache.remove(deviceId);
    }

    @Override
    public ToDevice getToDevice(String deviceId) {
        return cache.get(deviceId);
    }
}
