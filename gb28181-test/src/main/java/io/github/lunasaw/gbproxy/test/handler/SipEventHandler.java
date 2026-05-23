package io.github.lunasaw.gbproxy.test.handler;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.server.transmit.event.*;
import io.github.lunasaw.gbproxy.test.config.SipBusinessConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 业务事件监听示例：按需实现，无需空方法。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SipEventHandler {

    private final ServerCommandSender commandSender;
    private final SipBusinessConfig sessionCache;

    @EventListener
    public void onKeepalive(DeviceKeepaliveEvent event) {
        log.info("keepalive: {}", event.getDeviceId());
    }

    @EventListener
    public void onRemoteAddress(DeviceRemoteAddressEvent event) {
        // 设备地址变更时更新缓存
        String deviceId = event.getDeviceId();
        String ip = event.getRemoteAddressInfo().getIp();
        int port = event.getRemoteAddressInfo().getPort();
        sessionCache.register(deviceId, ip, port, "UDP");
    }

    @EventListener
    public void onCatalog(DeviceCatalogEvent event) {
        // sn 可用于关联之前发出的查询请求
        log.info("catalog response: deviceId={}, sn={}, channels={}",
            event.getDeviceId(), event.getSn(),
            event.getCatalog().getDeviceItemList() == null ? 0 : event.getCatalog().getDeviceItemList().size());
    }

    @EventListener
    public void onRecord(DeviceRecordEvent event) {
        log.info("record response: deviceId={}, sn={}", event.getDeviceId(), event.getSn());
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent event) {
        log.warn("alarm: deviceId={}", event.getDeviceId());
    }

    // 示例：收到目录响应后主动查询设备信息
    public void queryCatalog(String deviceId) {
        commandSender.deviceCatalogQuery(deviceId);
    }
}
