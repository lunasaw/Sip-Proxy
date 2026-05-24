package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.server.transmit.event.DeviceAlarmEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.DeviceRegisterEvent;
import io.github.lunasaw.gbproxy.server.transmit.event.ServerInviteEvent;

/**
 * 业务服务器通知接口。
 *
 * <p>sip-gateway 把入站 SIP 事件（注册、INVITE、告警等）转发给上游业务服务器，
 * 通常通过 HTTP/MQ 实现。框架不约束传输方式——业务方按需自行实现一个 Bean
 * 替换默认的 {@link NoopBusinessNotifier}。
 *
 * <p>注意：转发应该是异步的（推 MQ / 异步 HTTP），否则会阻塞 SIP 事件线程。
 */
public interface BusinessNotifier {

    void deviceOnline(DeviceRegisterEvent event);

    void inviteIncoming(ServerInviteEvent event);

    void alarm(DeviceAlarmEvent event);
}
