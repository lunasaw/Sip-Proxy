package io.github.lunasaw.gbproxy.server.transimit.request.register;

import io.github.lunasaw.sip.common.entity.SipTransaction;

import javax.sip.RequestEvent;

/**
 * Server模块REGISTER请求处理器业务接口
 * 负责具体的注册业务逻辑实现
 *
 * @author luna
 */
public interface ServerRegisterProcessorHandler {

    /**
     * 处理401未授权响应
     *
     * @param userId 用户ID
     * @param evt    请求事件
     */
    default void handleUnauthorized(String userId, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 获取设备事务信息
     *
     * @param userId 用户ID
     * @return 事务信息
     */
    default SipTransaction getDeviceTransaction(String userId) {
        return null;
    }

    /**
     * 处理注册信息更新
     *
     * @param userId       用户ID
     * @param registerInfo 注册信息
     * @param evt          请求事件
     */
    default void handleRegisterInfoUpdate(String userId, RegisterInfo registerInfo, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理SIP事务更新 - 设备上线
     *
     * @param userId         用户ID
     * @param sipTransaction SIP事务
     * @param evt            请求事件
     */
    default void handleDeviceOnline(String userId, SipTransaction sipTransaction, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 处理设备下线
     *
     * @param userId         用户ID
     * @param registerInfo   注册信息
     * @param sipTransaction SIP事务
     * @param evt            请求事件
     */
    default void handleDeviceOffline(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 获取设备过期时间
     *
     * @param userId 用户ID
     * @return 过期时间（秒）
     */
    default Integer getDeviceExpire(String userId) {
        return 3600; // 默认1小时
    }

    /**
     * 验证密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @param evt      请求事件
     * @return 是否验证成功
     */
    default boolean validatePassword(String userId, String password, RequestEvent evt) {
        return true; // 默认验证成功
    }
}