package io.github.lunasaw.gbproxy.client.transmit.request.info;

import javax.sip.RequestEvent;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspParser;
import io.github.lunasaw.gb28181.common.entity.mansrtsp.ManSrtspRequest;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInfoEvent;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.event.request.SipRequestProcessorAbstract;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端 INFO 请求处理器：直接回 200 OK 并发布 {@link ClientInfoEvent}。
 *
 * 当 Content-Type 为 Application/MANSRTSP 时，会解析 body 为结构化的
 * {@link ManSrtspRequest} 并附加到事件中（保留 raw 字符串以兼容）。
 *
 * @author luna
 */
@Component("clientInfoRequestProcessor")
@Getter
@Setter
@Slf4j
public class InfoRequestProcessor extends SipRequestProcessorAbstract {

    public static final String METHOD = "INFO";
    private static final String MANSRTSP_TYPE = "Application/MANSRTSP";

    private String method = METHOD;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void process(RequestEvent evt) {
        process(evt, evt.getServerTransaction());
    }

    @Override
    public void process(RequestEvent evt, javax.sip.ServerTransaction serverTransaction) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String userId = SipUtils.getUserIdFromToHeader(request);
            String content = request.getRawContent() != null ? new String(request.getRawContent()) : "";

            String contentType = null;
            ContentTypeHeader contentTypeHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
            if (contentTypeHeader != null) {
                contentType = contentTypeHeader.getContentType() + "/" + contentTypeHeader.getContentSubType();
            }

            ManSrtspRequest parsed = null;
            if (contentType != null && MANSRTSP_TYPE.equalsIgnoreCase(contentType)) {
                parsed = ManSrtspParser.parse(content);
            }

            publisher.publishEvent(new ClientInfoEvent(this, userId, content, contentType, parsed));

            try {
                ResponseCmd.sendResponse(Response.OK, evt, serverTransaction);
            } catch (Exception responseEx) {
                log.warn("INFO 200 OK 发送失败（业务事件已派发）: {}", responseEx.getMessage());
            }
        } catch (Exception e) {
            log.error("处理INFO请求异常: evt = {}", evt, e);
            try {
                ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, e.getMessage(), evt, serverTransaction);
            } catch (Exception ignore) {
                log.warn("INFO 500 响应发送失败: {}", ignore.getMessage());
            }
        }
    }
}
