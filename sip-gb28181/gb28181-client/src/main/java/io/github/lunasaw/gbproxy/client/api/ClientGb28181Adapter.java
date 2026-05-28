package io.github.lunasaw.gbproxy.client.api;

/**
 * 一站式适配基类：业务方继承它即可获得所有 hook（5 个 listener interface）。
 *
 * <p>等价于：{@code implements QueryListener, ControlListener, ConfigListener,
 * SubscribeListener, NotifyListener}。
 *
 * <p>按需选择粒度：
 * <ul>
 *   <li>需要全 hook 的业务方继承本基类，{@code @Override} 关心的方法</li>
 *   <li>只需要部分 hook 的业务方直接 {@code implements} 单个或几个 listener interface</li>
 * </ul>
 *
 * @author luna
 */
public abstract class ClientGb28181Adapter
        implements QueryListener, ControlListener, ConfigListener, SubscribeListener, NotifyListener {
}
