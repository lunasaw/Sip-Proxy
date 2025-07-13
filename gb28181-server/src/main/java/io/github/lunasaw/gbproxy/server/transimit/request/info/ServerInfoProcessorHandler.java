package io.github.lunasaw.gbproxy.server.transimit.request.info;

import javax.sip.RequestEvent;

/**
 * Server模块INFO请求处理器业务接口
 * 负责具体的INFO请求业务逻辑实现
 *
 * @author luna
 */
public interface ServerInfoProcessorHandler {

    /**
     * 处理INFO请求
     *
     * @param userId  用户ID
     * @param content 请求内容
     * @param evt     请求事件
     */
    default void handleInfoRequest(String userId, String content, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }

    /**
     * 验证设备权限
     *
     * @param userId 用户ID
     * @param sipId  SIP ID
     * @param evt    请求事件
     * @return 是否有权限
     */
    default boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) {
        return true; // 默认验证通过
    }

    /**
     * 处理INFO请求错误
     *
     * @param userId       用户ID
     * @param errorMessage 错误消息
     * @param evt          请求事件
     */
    default void handleInfoError(String userId, String errorMessage, RequestEvent evt) {
        // 默认实现为空，由业务方根据需要实现
    }
}