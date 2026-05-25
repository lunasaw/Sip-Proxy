package io.github.lunasaw.sip.common.transmit;

/**
 * dialog 内消息（BYE / re-INVITE / INFO / UPDATE / SUBSCRIBE refresh / unsubscribe）找不到对应
 * dialog 时抛出。
 *
 * <p>常见原因：
 * <ul>
 *   <li>INVITE / SUBSCRIBE 还未收到 200 OK，dialog 处于 Early 状态被取消</li>
 *   <li>对端先发 BYE / NOTIFY: terminated 终结了 dialog，本侧再发 dialog 内消息</li>
 *   <li>SUBSCRIBE 订阅自然超时未续订，被 cleanupExpired 清理</li>
 *   <li>callId 输入错误或已过期</li>
 * </ul>
 *
 * <p>区别于 {@code SipException}：这是<b>业务层调用错误</b>，不是网络/协议错误，
 * 应快速抛出，不做兜底（避免发出协议非法的 BYE / SUBSCRIBE 让设备返回 481，被掩盖成"已成功"）。
 *
 * @author luna
 */
public class DialogNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DialogNotFoundException(String message) {
        super(message);
    }
}
