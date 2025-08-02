package io.github.lunasaw.gbproxy.server.transmit.request.register;

import io.github.lunasaw.sip.common.entity.SipTransaction;
import lombok.extern.slf4j.Slf4j;

import javax.sip.RequestEvent;

/**
 * Server模块REGISTER请求处理器业务接口默认实现
 *
 * @author luna
 */
@Slf4j
public class DefaultServerRegisterProcessorHandler implements ServerRegisterProcessorHandler {

    @Override
    public void handleUnauthorized(String userId, RequestEvent evt) {
        log.debug("默认处理401未授权响应：用户ID = {}", userId);
    }

    @Override
    public SipTransaction getDeviceTransaction(String userId) {
        log.debug("默认获取设备事务信息：用户ID = {}", userId);
        return null;
    }

    @Override
    public void handleRegisterInfoUpdate(String userId, RegisterInfo registerInfo, RequestEvent evt) {
        log.debug("默认处理注册信息更新：用户ID = {}, 注册信息 = {}", userId, registerInfo);
    }

    @Override
    public void handleDeviceOnline(String userId, SipTransaction sipTransaction, RequestEvent evt) {
        log.debug("默认处理设备上线：用户ID = {}, 事务 = {}", userId, sipTransaction);
    }

    @Override
    public void handleDeviceOffline(String userId, RegisterInfo registerInfo, SipTransaction sipTransaction, RequestEvent evt) {
        log.debug("默认处理设备下线：用户ID = {}, 注册信息 = {}, 事务 = {}", userId, registerInfo, sipTransaction);
    }

    @Override
    public Integer getDeviceExpire(String userId) {
        log.debug("默认获取设备过期时间：用户ID = {}", userId);
        return 3600;
    }

    @Override
    public boolean validatePassword(String userId, String password, RequestEvent evt) {
        log.debug("默认验证密码：用户ID = {}", userId);
        return true;
    }
}