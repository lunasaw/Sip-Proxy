package io.github.lunasaw.gbproxy.test.config;

import com.google.common.collect.Lists;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.DeviceSupplier;
import io.github.lunasaw.sip.common.service.DefaultDeviceSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备初始化器
 * 用于在应用启动时初始化默认设备配置
 * 注意：服务端设备配置已移除，改为由业务方通过DeviceSupplier自定义
 *
 * @author luna
 * @date 2025/01/23
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(name = "deviceInitializer")
public class DeviceInitializer {

    @Autowired
    private DeviceSupplier      deviceSupplier;

    /**
     * 默认设备配置
     */
    private static final String DEFAULT_LOOP_IP       = "127.0.0.1";
    private static final String DEFAULT_LOOP_IP_LOCAL = "0.0.0.0";
    private static final String DEFAULT_REMOTE_IP     = "10.37.5.132";

    @PostConstruct
    public void initializeDevices() {
        log.info("开始初始化默认设备配置...");

        try {
            // 初始化默认设备
            initializeDefaultDevices();

            log.info("设备初始化完成，共初始化 {} 个设备", deviceSupplier.getDeviceCount());
        } catch (Exception e) {
            log.error("设备初始化失败", e);
        }
    }

    /**
     * 初始化默认设备配置
     * 注意：服务端设备配置已移除，改为由业务方自定义
     */
    private void initializeDefaultDevices() {
        Map<String, Device> devices = new HashMap<>();

        // 客户端设备配置
        FromDevice clientFrom = FromDevice.getInstance("33010602011187000001", DEFAULT_LOOP_IP, 8118);
        devices.put("clientFrom", clientFrom);

        ToDevice clientTo = ToDevice.getInstance("41010500002000000001", DEFAULT_LOOP_IP, 8117);
        clientTo.setPassword("bajiuwulian1006");
        clientTo.setRealm("4101050000");
        devices.put("clientTo", clientTo);

        // 注意：服务端设备配置已移除，改为由业务方通过DeviceSupplier自定义
        // 业务方可以在DefaultSipUserGenerateServer.getFromDevice()中实现自定义逻辑

        // 批量添加设备
        if (deviceSupplier instanceof DefaultDeviceSupplier) {
            ((DefaultDeviceSupplier)deviceSupplier).addDevices(Lists.newArrayList(devices.values()));
        } else {
            devices.values().forEach(deviceSupplier::addOrUpdateDevice);
        }

        log.info("默认设备配置初始化完成");
    }

    /**
     * 获取默认设备配置
     * 供外部调用，用于获取默认的设备配置信息
     * 注意：服务端设备配置已移除，改为由业务方自定义
     */
    public static Map<String, Device> getDefaultDevices() {
        Map<String, Device> devices = new HashMap<>();

        // 客户端设备配置
        FromDevice clientFrom = FromDevice.getInstance("33010602011187000001", DEFAULT_LOOP_IP, 8118);
        devices.put("clientFrom", clientFrom);

        ToDevice clientTo = ToDevice.getInstance("41010500002000000001", DEFAULT_LOOP_IP, 8117);
        clientTo.setPassword("bajiuwulian1006");
        clientTo.setRealm("4101050000");
        devices.put("clientTo", clientTo);

        // 注意：服务端设备配置已移除，改为由业务方自定义

        return devices;
    }
}