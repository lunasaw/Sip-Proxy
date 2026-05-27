package io.github.lunasaw.gbproxy.client.api;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceBroadcastNotify;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;

/**
 * 平台通知监听器（rootType=Notify，client 角色业务方实现）。
 *
 * <p>fire-and-forget。客户端方向核心场景是语音广播 Broadcast。
 *
 * @author luna
 */
public interface NotifyListener {

    /**
     * 语音广播通知（cmdType=Broadcast）。
     *
     * @param platformId 发送通知的上级平台编码
     * @param notify     语音广播通知内容
     */
    default void onBroadcastNotify(String platformId, DeviceBroadcastNotify notify) {}

    /**
     * 框架内部分发兜底：当 ClientNotifyEvent 携带的 notify 类不属于已知类型时调用。
     * 业务方一般无需 override；保留此 hook 用于诊断和未来 notify 子类的扩展。
     *
     * @param platformId 发送通知的上级平台编码
     * @param notify     未识别的通知 XML 对象
     */
    default void onUnknownNotify(String platformId, XmlBean notify) {}
}
