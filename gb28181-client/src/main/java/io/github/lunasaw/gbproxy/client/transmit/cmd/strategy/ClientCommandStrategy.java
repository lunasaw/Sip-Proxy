package io.github.lunasaw.gbproxy.client.transmit.cmd.strategy;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.transmit.event.Event;

/**
 * 客户端命令策略接口
 * 定义统一的命令执行策略，支持不同类型的GB28181命令
 *
 * @author luna
 * @date 2024/01/01
 */
public interface ClientCommandStrategy {

    /**
     * 执行命令
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param params     命令参数
     * @return callId
     */
    String execute(FromDevice fromDevice, ToDevice toDevice, Object... params);

    /**
     * 执行命令（带事件）
     *
     * @param fromDevice 发送设备
     * @param toDevice   接收设备
     * @param errorEvent 错误事件
     * @param okEvent    成功事件
     * @param params     命令参数
     * @return callId
     */
    String execute(FromDevice fromDevice, ToDevice toDevice, Event errorEvent, Event okEvent, Object... params);

    /**
     * 获取命令类型
     *
     * @return 命令类型
     */
    String getCommandType();

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    String getCommandDescription();
}