package io.github.lunasaw.gbproxy.gateway.notifier;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
import org.junit.jupiter.api.Test;

/**
 * 单元测试：默认 NoopBusinessNotifier 实现 BusinessNotifier 接口，
 * 所有方法允许 null 入参不抛异常（仅日志输出）。
 */
class NoopBusinessNotifierTest {

    @Test
    void implements_BusinessNotifier() {
        BusinessNotifier notifier = new NoopBusinessNotifier();
        // 编译期保证类型，运行时只是确认不抛
        notifier.deviceOnline("device-1", null);
        notifier.inviteIncoming("call-1", "from-1", "to-1", null, null, "ctx-1");
        notifier.alarm("device-1", new DeviceAlarmNotify());
    }

    @Test
    void allows_null_arguments_without_exception() {
        BusinessNotifier notifier = new NoopBusinessNotifier();
        notifier.deviceOnline(null, null);
        notifier.inviteIncoming(null, null, null, null, null, null);
        notifier.alarm(null, null);
    }
}
