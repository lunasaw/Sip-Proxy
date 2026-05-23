package io.github.lunasaw.sip.common.exception;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import lombok.extern.slf4j.Slf4j;

/**
 * SIP异常处理器
 * 提供统一的异常处理和错误响应生成
 *
 * @author luna
 * @date 2024/01/01
 */
@Slf4j
@Component
public class SipExceptionHandler {

    /**
     * 处理SIP异常并生成相应的响应
     *
     * @param exception 异常信息
     * @param requestEvent 原始请求事件，如果为null则只记录日志
     */
    public void handleException(SipException exception, RequestEvent requestEvent) {
        // 记录详细的异常信息
        log.error("SIP异常处理 - 错误类型: {}, 错误代码: {}, 异常信息: {}",
            exception.getErrorType().getDescription(),
            exception.getErrorCode(),
            exception.getMessage(),
            exception);

        // 如果有请求事件，生成相应的错误响应
        if (requestEvent != null) {
            int statusCode = mapToSipStatusCode(exception.getErrorType());
            String reasonPhrase = exception.getErrorType().getDescription();

            try {
                ResponseCmd.sendResponse(statusCode, reasonPhrase, requestEvent);
                log.info("已发送SIP错误响应: {} {}", statusCode, reasonPhrase);
            } catch (Exception e) {
                log.error("发送SIP错误响应失败", e);
            }
        }
    }

    /**
     * 处理一般异常
     *
     * @param exception 异常信息
     * @param requestEvent 原始请求事件
     * @param context 上下文信息
     */
    public void handleException(Exception exception, RequestEvent requestEvent, String context) {
        log.error("SIP处理异常 - 上下文: {}, 异常信息: {}", context, exception.getMessage(), exception);

        if (requestEvent != null) {
            try {
                ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, "内部服务器错误", requestEvent);
                log.info("已发送SIP错误响应: {} 内部服务器错误", Response.SERVER_INTERNAL_ERROR);
            } catch (Exception e) {
                log.error("发送SIP错误响应失败", e);
            }
        }
    }

    /**
     * 将SIP错误类型映射到SIP状态码
     *
     * @param errorType 错误类型
     * @return SIP状态码
     */
    private int mapToSipStatusCode(SipErrorType errorType) {
        switch (errorType) {
            case AUTHENTICATION_FAILED:
                return Response.UNAUTHORIZED;
            case PERMISSION_DENIED:
                return Response.FORBIDDEN;
            case DEVICE_NOT_FOUND:
                return Response.NOT_FOUND;
            case MESSAGE_FORMAT:
            case PROTOCOL_PARSE:
                return Response.BAD_REQUEST;
            case TIMEOUT:
                return Response.REQUEST_TIMEOUT;
            case RESOURCE_INSUFFICIENT:
                return Response.SERVICE_UNAVAILABLE;
            case DEVICE_OFFLINE:
                return Response.TEMPORARILY_UNAVAILABLE;
            case CONFIGURATION_ERROR:
            case SYSTEM_INTERNAL:
            case UNKNOWN:
            default:
                return Response.SERVER_INTERNAL_ERROR;
        }
    }

    /**
     * 创建SIP处理器异常
     *
     * @param processorName 处理器名称
     * @param sipMethod SIP方法
     * @param message 异常消息
     * @return SIP处理器异常
     */
    public static SipProcessorException createProcessorException(String processorName, String sipMethod, String message) {
        return new SipProcessorException(processorName, sipMethod, message);
    }

    /**
     * 创建SIP处理器异常
     *
     * @param processorName 处理器名称
     * @param sipMethod SIP方法
     * @param message 异常消息
     * @param cause 原因异常
     * @return SIP处理器异常
     */
    public static SipProcessorException createProcessorException(String processorName, String sipMethod, String message, Throwable cause) {
        return new SipProcessorException(processorName, sipMethod, message, cause);
    }
}