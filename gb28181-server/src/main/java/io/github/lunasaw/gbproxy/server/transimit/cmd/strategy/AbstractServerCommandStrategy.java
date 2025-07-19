package io.github.lunasaw.gbproxy.server.transimit.cmd.strategy;

import com.luna.common.check.Assert;
import com.luna.common.text.RandomStrUtil;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.event.Event;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象服务端命令策略基类
 * 提供通用的命令执行逻辑和工具方法
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
public abstract class AbstractServerCommandStrategy implements ServerCommandStrategy {

    @Override
    public String execute(FromDevice fromDevice, ToDevice toDevice, Object... params) {
        return execute(fromDevice, toDevice, null, null, params);
    }

    @Override
    public String execute(FromDevice fromDevice, ToDevice toDevice, Event errorEvent, Event okEvent, Object... params) {
        // 构建请求对象
        ServerCommandStrategyReq req = ServerCommandStrategyReq.builder()
                .fromDevice(fromDevice)
                .toDevice(toDevice)
                .errorEvent(errorEvent)
                .okEvent(okEvent)
                .build();

        // 将参数放入paramMap，第一个参数作为"content"，其他参数按索引命名
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (i == 0) {
                    req.getParamMap().put("content", params[i]);
                } else {
                    req.getParamMap().put("param" + i, params[i]);
                }
            }
        }

        return execute(req);
    }

    @Override
    public String execute(ServerCommandStrategyReq req) {
        try {
            Assert.notNull(req, "命令请求参数不能为空");
            Assert.notNull(req.getFromDevice(), "发送设备不能为空");
            Assert.notNull(req.getToDevice(), "接收设备不能为空");
            log.debug("执行命令: {}, 发送设备: {}, 接收设备: {}", getCommandType(), req.getFromDevice().getUserId(), req.getToDevice().getUserId());

            // 参数校验
            validateParams(req);

            // 构建命令内容
            String content = buildCommandContent(req);
            req.setContent(content);

            // 发送命令
            String callId = sendCommand(req);

            log.debug("命令执行成功: {}, callId: {}", getCommandType(), callId);
            return callId;

        } catch (Exception e) {
            log.error("命令执行失败: {}, 错误信息: {}", getCommandType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 参数校验
     *
     * @param req 命令请求参数
     */
    protected void validateParams(ServerCommandStrategyReq req) {
        Assert.notNull(req.getFromDevice(), "发送设备不能为空");
        Assert.notNull(req.getToDevice(), "接收设备不能为空");
        Assert.notNull(req.getFromDevice().getUserId(), "发送设备ID不能为空");
        Assert.notNull(req.getToDevice().getUserId(), "接收设备ID不能为空");
    }

    /**
     * 构建命令内容
     *
     * @param req 命令请求参数
     * @return 命令内容
     */
    protected String buildCommandContent(ServerCommandStrategyReq req) {
        // 默认实现：从paramMap中获取content参数
        Object content = req.getParamMap().get("content");
        if (content instanceof String) {
            return (String) content;
        }
        return null;
    }

    /**
     * 发送命令
     *
     * @param req 命令请求参数
     * @return callId
     */
    protected String sendCommand(ServerCommandStrategyReq req) {
        return SipSender.doMessageRequest(req.getFromDevice(), req.getToDevice(), req.getContent(), req.getErrorEvent(), req.getOkEvent());
    }

    /**
     * 生成随机序列号
     *
     * @return 序列号
     */
    protected String generateSn() {
        return RandomStrUtil.getValidationCode();
    }

    /**
     * 获取设备ID
     *
     * @param fromDevice 发送设备
     * @return 设备ID
     */
    protected String getDeviceId(FromDevice fromDevice) {
        return fromDevice.getUserId();
    }

    /**
     * 获取目标设备ID
     *
     * @param toDevice 接收设备
     * @return 设备ID
     */
    protected String getTargetDeviceId(ToDevice toDevice) {
        return toDevice.getUserId();
    }
}