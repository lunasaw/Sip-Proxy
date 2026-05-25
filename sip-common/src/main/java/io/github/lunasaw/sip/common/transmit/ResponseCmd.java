package io.github.lunasaw.sip.common.transmit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.CallInfo;
import org.apache.commons.lang3.StringUtils;

import com.luna.common.check.Assert;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.context.SipTransactionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SIP响应命令构建器（重构版）
 * 使用建造者模式提供流式API，支持事务和非事务响应
 *
 * @author luna
 * @date 2023/10/19
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseCmd {

    /**
     * SIP响应构建器
     * 提供流式API来构建和发送SIP响应
     */
    public static class SipResponseBuilder {
        private final int statusCode;
        private String phrase;
        private String content;
        private ContentTypeHeader contentTypeHeader;
        private List<Header> headers = new ArrayList<>();
        private RequestEvent requestEvent;
        private Request request;
        private ServerTransaction serverTransaction;
        private String ip;
        private boolean useTransaction = true;

        public SipResponseBuilder(int statusCode) {
            this.statusCode = statusCode;
        }

        /**
         * 设置响应短语
         */
        public SipResponseBuilder phrase(String phrase) {
            this.phrase = phrase;
            return this;
        }

        /**
         * 设置响应内容
         */
        public SipResponseBuilder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * 设置内容类型
         */
        public SipResponseBuilder contentType(ContentTypeHeader contentTypeHeader) {
            this.contentTypeHeader = contentTypeHeader;
            return this;
        }

        /**
         * 添加响应头
         */
        public SipResponseBuilder header(Header header) {
            this.headers.add(header);
            return this;
        }

        /**
         * 添加多个响应头
         */
        public SipResponseBuilder headers(Header... headers) {
            this.headers.addAll(Arrays.asList(headers));
            return this;
        }

        /**
         * 添加响应头列表
         */
        public SipResponseBuilder headers(List<Header> headers) {
            this.headers.addAll(headers);
            return this;
        }

        /**
         * 设置请求事件
         */
        public SipResponseBuilder requestEvent(RequestEvent requestEvent) {
            this.requestEvent = requestEvent;
            this.request = requestEvent.getRequest();
            return this;
        }

        /**
         * 设置请求
         */
        public SipResponseBuilder request(Request request) {
            this.request = request;
            return this;
        }

        /**
         * 设置服务器事务
         */
        public SipResponseBuilder serverTransaction(ServerTransaction serverTransaction) {
            this.serverTransaction = serverTransaction;
            return this;
        }

        /**
         * 设置IP地址
         */
        public SipResponseBuilder ip(String ip) {
            this.ip = ip;
            return this;
        }

        /**
         * 设置是否使用事务
         */
        public SipResponseBuilder useTransaction(boolean useTransaction) {
            this.useTransaction = useTransaction;
            return this;
        }

        /**
         * 构建并发送响应
         */
        public void send() {
            try {
                Response response = buildResponse();
                if (useTransaction) {
                    sendWithTransaction(response);
                } else {
                    sendWithoutTransaction(response);
                }
            } catch (Exception e) {
                log.error("发送SIP响应失败: statusCode={}, phrase={}", statusCode, phrase, e);
                throw new RuntimeException("发送SIP响应失败", e);
            }
        }

        /**
         * 构建响应对象
         */
        public Response buildResponse() {
            try {
                Assert.notNull(request, "请求不能为null");

                Response response = SipRequestUtils.createResponse(statusCode, request);

                if (StringUtils.isNotBlank(phrase)) {
                    response.setReasonPhrase(phrase);
                }

                if (StringUtils.isNotBlank(content)) {
                    response.setContent(content, contentTypeHeader);
                }

                SipRequestUtils.setResponseHeader(response, headers);

                // 确保响应使用与原请求相同的Call-ID（事务一致性）
                ensureCallIdConsistency(response);

                return response;
            } catch (Exception e) {
                log.error("构建响应对象失败: statusCode={}, phrase={}", statusCode, phrase, e);
                throw new RuntimeException("构建响应对象失败", e);
            }
        }

        /**
         * 确保Call-ID一致性
         * 如果存在事务上下文，则确保响应使用原请求的Call-ID
         */
        private void ensureCallIdConsistency(Response response) {
            // SIP响应必须与原始请求完全匹配，不能修改任何头部信息
            // MESSAGE_FACTORY.createResponse已经确保了完全的头部匹配
            // 这里只做调试验证，不做任何修改
            try {
                String contextCallId = SipTransactionContext.getCurrentCallId();
                if (StringUtils.isNotBlank(contextCallId)) {
                    // 验证响应的Call-ID是否与事务上下文一致
                    String responseCallId = ((CallID) response.getHeader("Call-ID")).getCallId();
                    if (!contextCallId.equals(responseCallId)) {
                        log.warn("响应Call-ID与事务上下文不一致: response={}, context={}, 但保持原请求Call-ID不变以确保事务匹配",
                                responseCallId, contextCallId);
                        // 关键：不修改Call-ID！响应必须与原始请求的事务完全匹配
                    } else {
                        log.debug("响应Call-ID与事务上下文一致: {}", contextCallId);
                    }
                }
            } catch (Exception e) {
                log.warn("检查Call-ID一致性时发生异常，将继续执行", e);
            }
        }

        /**
         * 使用事务发送响应
         */
        private void sendWithTransaction(Response response) {
            try {
                ServerTransaction transaction = getServerTransaction();
                if (transaction != null) {
                    // 验证事务与响应的匹配性
                    log.debug("发送事务响应: transaction={}, response-callId={}, response-cseq={}",
                            transaction,
                            response.getHeader("Call-ID"),
                            response.getHeader("CSeq"));
                    transaction.sendResponse(response);
                } else {
                    // 如果没有事务，降级到无事务模式
                    log.warn("无法获取服务器事务，降级到无事务模式发送响应");
                    sendWithoutTransaction(response);
                }
            } catch (Exception e) {
                log.error("使用事务发送响应失败，尝试降级到无事务模式", e);
                try {
                    sendWithoutTransaction(response);
                } catch (Exception fallbackException) {
                    log.error("无事务模式发送响应也失败", fallbackException);
                    throw new RuntimeException("发送SIP响应失败", e);
                }
            }
        }

        /**
         * 不使用事务发送响应
         */
        private void sendWithoutTransaction(Response response) {
            SIPRequest sipRequest = (SIPRequest) request;
            String targetIp = getTargetIp(sipRequest);
            SipSender.transmitRequest(targetIp, response);
        }

        /**
         * 获取服务器事务
         */
        private ServerTransaction getServerTransaction() {

            // 其次尝试从RequestEvent中获取事务
            if (requestEvent != null) {
                serverTransaction = requestEvent.getServerTransaction();
                if (serverTransaction != null) {
                    log.debug("从RequestEvent获取服务器事务: {}", serverTransaction);
                    return serverTransaction;
                }
                if (request == null) {
                    request = requestEvent.getRequest();
                }
            }

            // 关键修复：尝试从SIPRequest直接获取事务（NIST-SIP实现特定方法）
            if (request instanceof SIPRequest) {
                try {
                    // 使用NIST-SIP的内部方法直接从请求对象获取事务
                    Object transaction = ((SIPRequest) request).getTransaction();
                    if (transaction instanceof ServerTransaction) {
                        serverTransaction = (ServerTransaction) transaction;
                        log.debug("从SIPRequest直接获取服务器事务: {}", serverTransaction);
                        return serverTransaction;
                    } else if (transaction != null) {
                        log.warn("SIPRequest.getTransaction()返回了非ServerTransaction类型: {}", transaction.getClass());
                    }
                } catch (Exception e) {
                    log.debug("从SIPRequest获取事务时发生异常: {}", e.getMessage());
                }
            }

            // 禁止创建新事务！这会导致"Response does not belong to this transaction"错误
            // 因为新创建的事务与原始请求的Branch参数和SentBy信息不匹配
            return null;
        }
    }

    private static String getTargetIp(SIPRequest sipRequest) {
        String targetIp;
        if (sipRequest.getLocalAddress() != null) {
            targetIp = sipRequest.getLocalAddress().getHostAddress();
        } else {
            // 如果本地地址为空，尝试从Via头获取地址
            ViaHeader viaHeader = (ViaHeader) sipRequest.getHeader(ViaHeader.NAME);
            if (viaHeader != null) {
                targetIp = viaHeader.getHost();
            } else {
                // 如果Via头也为空，使用默认地址
                targetIp = "127.0.0.1";
            }
        }
        return targetIp;
    }

    // ==================== 便捷方法 ====================

    /**
     * 创建响应构建器
     */
    public static SipResponseBuilder response(int statusCode) {
        return new SipResponseBuilder(statusCode);
    }

    /**
     * 快速发送简单响应（使用预创建的事务）
     *
     * @param statusCode        状态码
     * @param requestEvent      请求事件
     * @param serverTransaction 预创建的服务器事务
     */
    public static void sendResponse(int statusCode, RequestEvent requestEvent, ServerTransaction serverTransaction) {
        response(statusCode)
                .requestEvent(requestEvent)
                .serverTransaction(serverTransaction)
                .send();
    }

    /**
     * 快速发送带短语的响应（使用预创建的事务）
     *
     * @param statusCode        状态码
     * @param phrase            响应短语
     * @param requestEvent      请求事件
     * @param serverTransaction 预创建的服务器事务
     */
    public static void sendResponse(int statusCode, String phrase, RequestEvent requestEvent, ServerTransaction serverTransaction) {
        response(statusCode)
                .phrase(phrase)
                .requestEvent(requestEvent)
                .serverTransaction(serverTransaction)
                .send();
    }

    /**
     * 快速发送带内容的响应（使用预创建的事务）
     *
     * @param statusCode        状态码
     * @param content           响应内容
     * @param contentTypeHeader 内容类型头
     * @param requestEvent      请求事件
     * @param serverTransaction 预创建的服务器事务
     */
    public static void sendResponse(int statusCode, String content, ContentTypeHeader contentTypeHeader, RequestEvent requestEvent, ServerTransaction serverTransaction) {
        response(statusCode)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(requestEvent)
                .serverTransaction(serverTransaction)
                .send();
    }

    /**
     * 快速发送简单响应（使用事务）
     */
    public static void sendResponse(int statusCode, RequestEvent requestEvent) {
        response(statusCode)
                .requestEvent(requestEvent)
                .send();
    }

    /**
     * 快速发送带短语的响应（使用事务）
     */
    public static void sendResponse(int statusCode, String phrase, RequestEvent requestEvent) {
        response(statusCode)
                .phrase(phrase)
                .requestEvent(requestEvent)
                .send();
    }

    /**
     * 快速发送带内容的响应（使用事务）
     */
    public static void sendResponse(int statusCode, String content, ContentTypeHeader contentTypeHeader, RequestEvent requestEvent) {
        response(statusCode)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(requestEvent)
                .send();
    }

    /**
     * 快速发送简单响应（不使用事务）
     */
    public static void sendResponseNoTransaction(int statusCode, RequestEvent requestEvent) {
        response(statusCode)
                .requestEvent(requestEvent)
                .useTransaction(false)
                .send();
    }

    /**
     * 快速发送带短语的响应（不使用事务）
     */
    public static void sendResponseNoTransaction(int statusCode, String phrase, RequestEvent requestEvent) {
        response(statusCode)
                .phrase(phrase)
                .requestEvent(requestEvent)
                .useTransaction(false)
                .send();
    }

    /**
     * 快速发送带内容的响应（不使用事务）
     */
    public static void sendResponseNoTransaction(int statusCode, String content, ContentTypeHeader contentTypeHeader, RequestEvent requestEvent) {
        response(statusCode)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(requestEvent)
                .useTransaction(false)
                .send();
    }

    // ==================== 兼容性方法 ====================

    public static void sendResponse(int statusCode, String content, ContentTypeHeader contentTypeHeader, RequestEvent event, Header... headers) {
        response(statusCode)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(event)
                .headers(headers)
                .send();
    }

    public static void doResponseCmd(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader, RequestEvent event,
        List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(event)
                .headers(headers)
                .send();
    }

    public static void doResponseCmd(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader, String ip, Request request, List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .request(request)
                .ip(ip)
                .headers(headers)
                .send();
    }
}
