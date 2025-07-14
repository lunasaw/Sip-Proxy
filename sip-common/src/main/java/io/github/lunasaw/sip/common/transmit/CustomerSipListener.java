package io.github.lunasaw.sip.common.transmit;

import lombok.extern.slf4j.Slf4j;

/**
 * SIP信令处理类观察者
 * 继承AbstractSipListener，提供默认的SIP事件处理实现
 *
 * @author luna
 */
@Slf4j
public class CustomerSipListener extends AbstractSipListener {

    /**
     * 单实例
     */
    private static volatile AbstractSipListener instance;

    /**
     * 私有构造函数
     */
    private CustomerSipListener() {
        // 私有构造函数，防止外部实例化
    }

    /**
     * 获取单实例
     *
     * @return SipListener实例
     */
    public static AbstractSipListener getInstance() {
        if (instance == null) {
            synchronized (CustomerSipListener.class) {
                if (instance == null) {
                    instance = new CustomerSipListener();
                }
            }
        }
        return instance;
    }
}
