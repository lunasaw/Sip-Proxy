package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.impl;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.gbproxy.client.transmit.cmd.strategy.AbstractClientCommandStrategy;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * REGISTER消息类型策略实现
 * 处理REGISTER注册请求相关命令
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public class RegisterCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    protected String buildCommandContent(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        // REGISTER命令不需要构建内容，expires参数直接传递给SipSender
        return null;
    }

    @Override
    public String getCommandType() {
        return "REGISTER";
    }

    @Override
    public String getCommandDescription() {
        return "REGISTER注册请求";
    }

    @Override
    protected String sendCommand(FromDevice fromDevice, ToDevice toDevice, String content, Event errorEvent, Event okEvent) {
        // 从参数中获取expires值
        Integer expires = getExpiresFromParams();
        if (expires == null) {
            expires = 3600; // 默认过期时间
        }
        
        // 发送REGISTER请求
        return SipSender.doRegisterRequest(fromDevice, toDevice, expires);
    }

    @Override
    public String execute(FromDevice fromDevice, ToDevice toDevice, Event errorEvent, Event okEvent, Object... params) {
        try {
            // 参数校验
            validateParams(fromDevice, toDevice, params);
            
            // 获取过期时间参数
            Integer expires = extractExpires(params);
            
            // 直接调用SipSender发送REGISTER请求
            String callId = SipSender.doRegisterRequest(fromDevice, toDevice, expires);
            
            log.debug("REGISTER命令执行成功, callId: {}, expires: {}", callId, expires);
            return callId;
            
        } catch (Exception e) {
            log.error("REGISTER命令执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void validateParams(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        super.validateParams(fromDevice, toDevice, params);
        // REGISTER命令需要expires参数
        if (params.length == 0) {
            throw new IllegalArgumentException("REGISTER命令需要提供expires参数");
        }
        
        // 验证expires参数类型
        Object expiresParam = params[0];
        if (!(expiresParam instanceof Integer)) {
            throw new IllegalArgumentException("expires参数必须是Integer类型");
        }
        
        Integer expires = (Integer) expiresParam;
        if (expires < 0) {
            throw new IllegalArgumentException("expires参数必须 >= 0");
        }
    }

    /**
     * 从参数中提取expires值
     */
    private Integer extractExpires(Object... params) {
        if (params.length > 0 && params[0] instanceof Integer) {
            return (Integer) params[0];
        }
        return 3600; // 默认值
    }

    /**
     * 临时变量存储expires，用于传递给sendCommand方法
     */
    private Integer currentExpires;
    
    private Integer getExpiresFromParams() {
        return currentExpires;
    }
}