package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAlarmEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 {@link BusinessNotifier}：仅打印日志，便于演示与本地开发。
 *
 * <p>生产环境覆盖此 Bean 改为 HTTP/MQ 推送即可。
 */
@Slf4j
public class NoopBusinessNotifier implements BusinessNotifier {

    @Override
    public void deviceOnline(DeviceRegisterEvent event) {
        log.info("[business] device online: {}", event.getDeviceId());
    }

    @Override
    public void inviteIncoming(ServerInviteEvent event) {
        log.info("[business] invite incoming: callId={}, from={}, to={}",
                event.getCallId(), event.getFromUserId(), event.getToUserId());
    }

    @Override
    public void alarm(DeviceAlarmEvent event) {
        log.warn("[business] alarm: deviceId={}", event.getDeviceId());
    }
}
