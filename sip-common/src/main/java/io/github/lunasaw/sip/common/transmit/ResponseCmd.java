package io.github.lunasaw.sip.common.transmit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.lang3.StringUtils;

import com.luna.common.check.Assert;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
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

                return response;
            } catch (Exception e) {
                log.error("构建响应对象失败: statusCode={}, phrase={}", statusCode, phrase, e);
                throw new RuntimeException("构建响应对象失败", e);
            }
        }

        /**
         * 使用事务发送响应
         */
        private void sendWithTransaction(Response response) {
            try {
                ServerTransaction transaction = getServerTransaction();
                if (transaction != null) {
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
            if (serverTransaction != null) {
                return serverTransaction;
            }

            if (requestEvent != null) {
                serverTransaction = requestEvent.getServerTransaction();
                if (serverTransaction != null) {
                    return serverTransaction;
                }
            }

            // 如果没有现有事务，尝试创建新事务
            try {
                String targetIp = ip;
                if (StringUtils.isBlank(targetIp)) {
                    SIPRequest sipRequest = (SIPRequest) request;
                    // 安全获取本地地址，避免NullPointerException
                    targetIp = getTargetIp(sipRequest);
                }

                return SipSender.getServerTransaction(request, targetIp);
            } catch (Exception e) {
                log.warn("无法创建服务器事务: {}", e.getMessage());
                return null;
            }
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

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmdNoTransaction(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader,
        RequestEvent request) {
        SipURI sipURI = (SipURI) request.getRequest().getRequestURI();
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(sipURI.getUser(), sipURI.getHost() + ":" + sipURI.getPort());
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(request)
                .header(contactHeader)
                .useTransaction(false)
                .send();
    }

    /**
     * @deprecated 使用 {@link #sendResponseNoTransaction(int, RequestEvent)} 替代
     */
    @Deprecated
    public static void doResponseCmdNoTransaction(int statusCode, RequestEvent request) {
        sendResponseNoTransaction(statusCode, request);
    }

    /**
     * @deprecated 使用 {@link #sendResponse(int, RequestEvent)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, RequestEvent request) {
        sendResponse(statusCode, request);
    }

    /**
     * @deprecated 使用 {@link #sendResponse(int, String, RequestEvent)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, String phrase, RequestEvent request) {
        sendResponse(statusCode, phrase, request);
    }

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, String phrase, RequestEvent request, Header... headers) {
        response(statusCode)
                .phrase(phrase)
                .requestEvent(request)
                .headers(headers)
                .send();
    }

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, String phrase, RequestEvent event, List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .requestEvent(event)
                .headers(headers)
                .send();
    }


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

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmdNoTransaction(int statusCode, String phrase, Request request, List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .request(request)
                .headers(headers)
                .useTransaction(false)
                .send();
    }

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmdNoTransaction(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader, Request request,
        List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .request(request)
                .headers(headers)
                .useTransaction(false)
                .send();
    }

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader, Request request, ServerTransaction serverTransaction, List<Header> headers) {
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .request(request)
                .serverTransaction(serverTransaction)
                .headers(headers)
                .send();
    }

    /**
     * @deprecated 使用 {@link #response(int)} 替代
     */
    @Deprecated
    public static void doResponseCmd(int statusCode, String phrase, String content, ContentTypeHeader contentTypeHeader, RequestEvent evt) {
        SipURI sipURI = (SipURI) evt.getRequest().getRequestURI();
        ContactHeader contactHeader = SipRequestUtils.createContactHeader(sipURI.getUser(), sipURI.getHost() + ":" + sipURI.getPort());
        response(statusCode)
                .phrase(phrase)
                .content(content)
                .contentType(contentTypeHeader)
                .requestEvent(evt)
                .header(contactHeader)
                .send();
    }
}
