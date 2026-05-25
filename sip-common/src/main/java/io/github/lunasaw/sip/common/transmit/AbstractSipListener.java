package io.github.lunasaw.sip.common.transmit;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.sip.common.context.SipTransactionContext;
import io.github.lunasaw.sip.common.metrics.SipMetrics;
import io.github.lunasaw.sip.common.transmit.event.Event;
import io.github.lunasaw.sip.common.transmit.event.EventResult;
import io.github.lunasaw.sip.common.transmit.event.SipSubscribe;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessor;
import io.github.lunasaw.sip.common.transmit.event.response.SipResponseProcessor;
import io.github.lunasaw.sip.common.transmit.event.timeout.ITimeoutProcessor;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.skywalking.apm.toolkit.trace.Trace;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIP监听器抽象基类
 * 提供基础统一的SIP事件处理能力，支持自定义Processor的添加
 *
 * @author luna
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractSipListener implements SipListener {


    /**
     * 对SIP事件进行处理
     */
    protected static final Map<String, List<SipRequestProcessor>> REQUEST_PROCESSOR_MAP = new ConcurrentHashMap<>();
    ;
    /**
     * 处理接收SIP发来的SIP协议响应消息
     */
    protected static final Map<String, List<SipResponseProcessor>> RESPONSE_PROCESSOR_MAP = new ConcurrentHashMap<>();
    /**
     * 处理超时事件
     */
    protected static final Map<String, List<ITimeoutProcessor>> TIMEOUT_PROCESSOR_MAP = new ConcurrentHashMap<>();

    /**
     * SIP指标收集器
     */
    @Getter
    @Setter
    protected SipMetrics sipMetrics = new SipMetrics(new SimpleMeterRegistry());


    /**
     * 添加 request订阅
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void addRequestProcessor(String method, SipRequestProcessor processor) {
        if (REQUEST_PROCESSOR_MAP.containsKey(method)) {
            List<SipRequestProcessor> processors = REQUEST_PROCESSOR_MAP.get(method);
            processors.add(processor);
        } else {
            List<SipRequestProcessor> processors = new ArrayList<>();
            processors.add(processor);
            REQUEST_PROCESSOR_MAP.put(method, processors);
        }
        log.info("添加请求处理器: {} -> {}", method, processor.getClass().getSimpleName());
    }

    /**
     * 添加 response订阅
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void addResponseProcessor(String method, SipResponseProcessor processor) {
        if (RESPONSE_PROCESSOR_MAP.containsKey(method)) {
            List<SipResponseProcessor> processors = RESPONSE_PROCESSOR_MAP.get(method);
            processors.add(processor);
        } else {
            List<SipResponseProcessor> processors = new ArrayList<>();
            processors.add(processor);
            RESPONSE_PROCESSOR_MAP.put(method, processors);
        }
        log.debug("添加响应处理器: {} -> {}", method, processor.getClass().getSimpleName());
    }

    /**
     * 添加 超时事件订阅
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void addTimeoutProcessor(String method, ITimeoutProcessor processor) {
        if (TIMEOUT_PROCESSOR_MAP.containsKey(method)) {
            List<ITimeoutProcessor> processors = TIMEOUT_PROCESSOR_MAP.get(method);
            processors.add(processor);
        } else {
            List<ITimeoutProcessor> processors = new ArrayList<>();
            processors.add(processor);
            TIMEOUT_PROCESSOR_MAP.put(method, processors);
        }
        log.debug("添加超时处理器: {} -> {}", method, processor.getClass().getSimpleName());
    }

    /**
     * 移除请求处理器
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void removeRequestProcessor(String method, SipRequestProcessor processor) {
        List<SipRequestProcessor> processors = REQUEST_PROCESSOR_MAP.get(method);
        if (processors != null) {
            processors.remove(processor);
            if (processors.isEmpty()) {
                REQUEST_PROCESSOR_MAP.remove(method);
            }
            log.debug("移除请求处理器: {} -> {}", method, processor.getClass().getSimpleName());
        }
    }

    /**
     * 移除响应处理器
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void removeResponseProcessor(String method, SipResponseProcessor processor) {
        List<SipResponseProcessor> processors = RESPONSE_PROCESSOR_MAP.get(method);
        if (processors != null) {
            processors.remove(processor);
            if (processors.isEmpty()) {
                RESPONSE_PROCESSOR_MAP.remove(method);
            }
            log.debug("移除响应处理器: {} -> {}", method, processor.getClass().getSimpleName());
        }
    }

    /**
     * 移除超时处理器
     *
     * @param method    方法名
     * @param processor 处理程序
     */
    public synchronized void removeTimeoutProcessor(String method, ITimeoutProcessor processor) {
        List<ITimeoutProcessor> processors = TIMEOUT_PROCESSOR_MAP.get(method);
        if (processors != null) {
            processors.remove(processor);
            if (processors.isEmpty()) {
                TIMEOUT_PROCESSOR_MAP.remove(method);
            }
            log.debug("移除超时处理器: {} -> {}", method, processor.getClass().getSimpleName());
        }
    }

    /**
     * 获取请求处理器列表
     *
     * @param method 方法名
     * @return 处理器列表
     */
    public List<SipRequestProcessor> getRequestProcessors(String method) {
        return REQUEST_PROCESSOR_MAP.get(method);
    }

    /**
     * 获取响应处理器列表
     *
     * @param method 方法名
     * @return 处理器列表
     */
    public List<SipResponseProcessor> getResponseProcessors(String method) {
        return RESPONSE_PROCESSOR_MAP.get(method);
    }

    /**
     * 获取超时处理器列表
     *
     * @param method 方法名
     * @return 处理器列表
     */
    public List<ITimeoutProcessor> getTimeoutProcessors(String method) {
        return TIMEOUT_PROCESSOR_MAP.get(method);
    }

    /**
     * 分发RequestEvent事件 - 基础实现
     *
     * @param requestEvent RequestEvent事件
     */
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Timer.Sample sample = sipMetrics != null ? sipMetrics.startTimer() : null;
        String method = requestEvent.getRequest().getMethod();
        ServerTransaction serverTransaction = null;

        try {
            // 全局注入SIP事务上下文 - 在最顶层入口处注入完整事务信息
            try {
                SipTransactionContext.SipTransactionInfo transactionInfo =
                        SipTransactionContext.SipTransactionInfo.fromRequestEvent(requestEvent);
                if (transactionInfo != null) {
                    SipTransactionContext.setTransactionInfo(transactionInfo);
                    log.debug("全局注入SIP事务上下文: {}, method={}, thread={}",
                            transactionInfo, method, Thread.currentThread().getName());
                } else {
                    log.warn("无法提取SIP事务信息，方法: {}", method);
                }
            } catch (Exception e) {
                log.warn("全局注入SIP事务上下文失败: method={}, error={}", method, e.getMessage());
            }
            // 记录方法调用
            if (sipMetrics != null) {
                sipMetrics.recordMethodCall(method);
            }

            List<SipRequestProcessor> sipRequestProcessors = REQUEST_PROCESSOR_MAP.get(method);
            if (CollectionUtils.isEmpty(sipRequestProcessors)) {
                log.warn("暂不支持方法 {} 的请求", method);
                if (sipMetrics != null) {
                    sipMetrics.recordError("UNSUPPORTED_METHOD", method);
                }
                // 调用子类自定义处理
                handleUnsupportedRequest(requestEvent);
                return;
            }

            // 对于需要事务的请求方法，立即创建服务器事务
            if (shouldCreateTransaction(method)) {
                try {
                    serverTransaction = requestEvent.getServerTransaction();
                    if (serverTransaction == null) {
                        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
                        serverTransaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
                        log.debug("为方法 {} 创建服务器事务: {}", method, serverTransaction);
                    }
                } catch (TransactionAlreadyExistsException e) {
                    // 事务已存在，重新获取现有事务
                    log.debug("事务已存在，使用现有事务: {}", e.getMessage());
                    try {
                        serverTransaction = requestEvent.getServerTransaction();
                        if (serverTransaction != null) {
                            log.debug("成功获取现有服务器事务: {}", serverTransaction);
                        } else {
                            log.warn("获取现有服务器事务失败，事务为null");
                        }
                    } catch (Exception ex) {
                        log.warn("重新获取现有事务时发生异常: {}", ex.getMessage());
                        serverTransaction = null;
                    }
                } catch (TransactionUnavailableException e) {
                    log.warn("事务不可用，方法 {}: {}", method, e.getMessage());
                    serverTransaction = null;
                } catch (Exception e) {
                    log.warn("为方法 {} 创建服务器事务时发生未知异常: {}", method, e.getMessage());
                    // 尝试获取现有事务作为回退策略
                    try {
                        serverTransaction = requestEvent.getServerTransaction();
                        if (serverTransaction != null) {
                            log.debug("回退策略成功，使用现有事务: {}", serverTransaction);
                        }
                    } catch (Exception ex) {
                        log.warn("回退策略失败: {}", ex.getMessage());
                        serverTransaction = null;
                    }
                }
            }

            // 同步处理请求，传入预创建的事务
            for (SipRequestProcessor sipRequestProcessor : sipRequestProcessors) {
                // 使用新的重载方法，支持传入服务器事务
                sipRequestProcessor.process(requestEvent, serverTransaction);
            }

            if (sipMetrics != null) {
                sipMetrics.recordMessageProcessed();
            }

        } catch (Exception e) {
            log.error("processRequest::requestEvent = {} ", requestEvent, e);
            if (sipMetrics != null) {
                sipMetrics.recordError("PROCESSING_ERROR", method);
                sipMetrics.recordMessageProcessed(method, "ERROR");
            }
            // 调用子类异常处理
            handleRequestException(requestEvent, e);
        } finally {
            // 清理SIP事务上下文，避免内存泄漏
            try {
                SipTransactionContext.clear();
                log.debug("清理SIP事务上下文: method={}, thread={}",
                        method, Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("清理SIP事务上下文失败: method={}, error={}", method, e.getMessage());
            }
            
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordRequestProcessingTime(sample);
            }
        }
    }

    /**
     * 分发ResponseEvent事件 - 基础实现
     *
     * @param responseEvent responseEvent事件
     */
    @Override
    public void processResponse(ResponseEvent responseEvent) {
        // 测试钩子：捕获401 REGISTER响应
        Timer.Sample sample = sipMetrics != null ? sipMetrics.startTimer() : null;
        Response response = responseEvent.getResponse();
        int status = response.getStatusCode();
        try {
            // Success
            if (((status >= Response.OK) && (status < Response.MULTIPLE_CHOICES)) || status == Response.UNAUTHORIZED) {
                CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
                String method = cseqHeader.getMethod();

                if (sipMetrics != null) {
                    sipMetrics.recordMethodCall(method + "_RESPONSE");
                }

                List<SipResponseProcessor> sipResponseProcessors = RESPONSE_PROCESSOR_MAP.get(method);
                if (CollectionUtils.isNotEmpty(sipResponseProcessors)) {
                    for (SipResponseProcessor sipResponseProcessor : sipResponseProcessors) {
                        if (sipResponseProcessor.isNeedProcess(responseEvent)) {
                            sipResponseProcessor.process(responseEvent);
                        }
                    }
                }

                if (status != Response.UNAUTHORIZED && responseEvent.getResponse() != null && SipSubscribe.getOkSubscribesSize() > 0) {
                    SipSubscribe.publishOkEvent(responseEvent);
                }

                if (sipMetrics != null) {
                    sipMetrics.recordMessageProcessed("RESPONSE", "SUCCESS");
                }

            } else if ((status >= Response.TRYING) && (status < Response.OK)) {
                // 增加其它无需回复的响应，如101、180等
                if (sipMetrics != null) {
                    sipMetrics.recordMessageProcessed("RESPONSE", "PROVISIONAL");
                }

            } else {
                log.warn("接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase() + " response = {}",
                        responseEvent.getResponse());
                if (sipMetrics != null) {
                    sipMetrics.recordError("FAILED_RESPONSE", String.valueOf(status));
                }

                if (responseEvent.getResponse() != null && SipSubscribe.getErrorSubscribesSize() > 0) {
                    CallIdHeader callIdHeader = (CallIdHeader) responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                    if (callIdHeader != null) {
                        Event subscribe = SipSubscribe.getErrorSubscribe(callIdHeader.getCallId());
                        if (subscribe != null) {
                            EventResult eventResult = new EventResult(responseEvent);
                            subscribe.response(eventResult);
                            SipSubscribe.removeErrorSubscribe(callIdHeader.getCallId());
                        }
                    }
                }
                if (responseEvent.getDialog() != null) {
                    responseEvent.getDialog().delete();
                }

                if (sipMetrics != null) {
                    sipMetrics.recordMessageProcessed("RESPONSE", "ERROR");
                }
            }

        } catch (Exception e) {
            log.error("processResponse error", e);
            if (sipMetrics != null) {
                sipMetrics.recordError("RESPONSE_PROCESSING_ERROR", String.valueOf(status));
            }
            // 调用子类异常处理
            handleResponseException(responseEvent, e);
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordResponseProcessingTime(sample);
            }
        }
    }

    /**
     * 向超时订阅发送消息 - 基础实现
     *
     * @param timeoutEvent timeoutEvent事件
     */
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        Timer.Sample sample = sipMetrics != null ? sipMetrics.startTimer() : null;
        ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();

        if (clientTransaction == null) {
            return;
        }

        Request request = clientTransaction.getRequest();
        if (request == null) {
            return;
        }

        try {
            CSeqHeader cseqHeader = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
            String method = cseqHeader.getMethod();

            if (sipMetrics != null) {
                sipMetrics.recordMethodCall(method + "_TIMEOUT");
            }

            List<ITimeoutProcessor> timeoutProcessors = TIMEOUT_PROCESSOR_MAP.get(method);
            if (CollectionUtils.isNotEmpty(timeoutProcessors)) {
                for (ITimeoutProcessor timeoutProcessor : timeoutProcessors) {
                    timeoutProcessor.process(timeoutEvent);
                }
            }

            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (callIdHeader != null) {
                Event subscribe = SipSubscribe.getErrorSubscribe(callIdHeader.getCallId());
                EventResult eventResult = new EventResult(timeoutEvent);
                if (subscribe != null) {
                    subscribe.response(eventResult);
                }
                SipSubscribe.removeOkSubscribe(callIdHeader.getCallId());
                SipSubscribe.removeErrorSubscribe(callIdHeader.getCallId());
            }

            if (sipMetrics != null) {
                sipMetrics.recordMessageProcessed("TIMEOUT", "SUCCESS");
            }

        } catch (Exception e) {
            log.error("processTimeout error", e);
            if (sipMetrics != null) {
                sipMetrics.recordError("TIMEOUT_PROCESSING_ERROR", "TIMEOUT");
            }
        } finally {
            if (sipMetrics != null && sample != null) {
                sipMetrics.recordTimeoutProcessingTime(sample);
            }
        }
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.error("processIOException::exceptionEvent = {} ", JSON.toJSONString(exceptionEvent));
        // 调用子类异常处理
        handleIOException(exceptionEvent);
    }

    /**
     * 事物结束 - 基础实现
     *
     * @param timeoutEvent -- an event that indicates that the
     *                     transaction has transitioned into the terminated state.
     */
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent timeoutEvent) {
        EventResult eventResult = new EventResult(timeoutEvent);

        Event timeOutSubscribe = SipSubscribe.getErrorSubscribe(eventResult.getCallId());
        if (timeOutSubscribe != null) {
            timeOutSubscribe.response(eventResult);
        }
    }

    /**
     * 会话结束 - 基础实现
     *
     * <p>1.7.0：dialog 终结（BYE / dialog 自然超时）时同步清理 {@link DialogRegistry}，
     * 防止出站 dialog 注册项泄漏。SUBSCRIBE 自然过期不走此路径，由
     * {@link DialogRegistry#cleanupExpired()} 兜底。
     *
     * @param dialogTerminatedEvent -- an event that indicates that the
     *                              dialog has transitioned into the terminated state.
     */
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        Dialog dialog = dialogTerminatedEvent.getDialog();
        if (dialog != null) {
            CallIdHeader callIdHeader = dialog.getCallId();
            String callId = callIdHeader != null ? callIdHeader.getCallId() : null;
            if (callId != null) {
                Dialog removed = DialogRegistry.remove(callId);
                if (removed != null) {
                    log.debug("DialogTerminatedEvent 清理 DialogRegistry: callId={}", callId);
                }
            }
        }

        EventResult eventResult = new EventResult(dialogTerminatedEvent);

        Event timeOutSubscribe = SipSubscribe.getErrorSubscribe(eventResult.getCallId());
        if (timeOutSubscribe != null) {
            timeOutSubscribe.response(eventResult);
        }
    }

    /**
     * 判断是否需要为特定方法创建服务器事务
     *
     * @param method SIP方法名
     * @return 是否需要创建事务
     */
    protected boolean shouldCreateTransaction(String method) {
        // MESSAGE、INFO、NOTIFY等需要响应的方法需要事务支持
        // ACK方法通常不需要服务器事务
        return !"ACK".equals(method);
    }

    // ==================== 子类可重写的方法 ====================

    /**
     * 处理不支持的请求（子类可重写）
     *
     * @param requestEvent 请求事件
     */
    protected void handleUnsupportedRequest(RequestEvent requestEvent) {
        // 默认实现为空，子类可重写
    }

    /**
     * 处理请求异常（子类可重写）
     *
     * @param requestEvent 请求事件
     * @param exception    异常
     */
    protected void handleRequestException(RequestEvent requestEvent, Exception exception) {
        // 默认实现为空，子类可重写
    }

    /**
     * 处理响应异常（子类可重写）
     *
     * @param responseEvent 响应事件
     * @param exception     异常
     */
    protected void handleResponseException(ResponseEvent responseEvent, Exception exception) {
        // 默认实现为空，子类可重写
    }

    /**
     * 处理IO异常（子类可重写）
     *
     * @param exceptionEvent IO异常事件
     */
    protected void handleIOException(IOExceptionEvent exceptionEvent) {
        // 默认实现为空，子类可重写
    }

    /**
     * 获取处理器统计信息
     *
     * @return 统计信息
     */
    public String getProcessorStats() {
        int requestProcessorCount = REQUEST_PROCESSOR_MAP.values().stream()
                .mapToInt(List::size)
                .sum();
        int responseProcessorCount = RESPONSE_PROCESSOR_MAP.values().stream()
                .mapToInt(List::size)
                .sum();
        int timeoutProcessorCount = TIMEOUT_PROCESSOR_MAP.values().stream()
                .mapToInt(List::size)
                .sum();

        return String.format("RequestProcessors: %d (methods: %d), ResponseProcessors: %d (methods: %d), TimeoutProcessors: %d (methods: %d)",
                requestProcessorCount, REQUEST_PROCESSOR_MAP.size(),
                responseProcessorCount, RESPONSE_PROCESSOR_MAP.size(),
                timeoutProcessorCount, TIMEOUT_PROCESSOR_MAP.size());
    }
}