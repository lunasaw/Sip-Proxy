package io.github.lunasaw.sipgateway.gb28181.web;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
import io.github.lunasaw.sipgateway.gb28181.dto.InviteResponseRequest;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContext;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sip.message.Response;

/**
 * GB28181 INVITE 异步回包控制器。
 *
 * <p>路径：{@code POST /gateway/gb28181/invite/response}
 *
 * <p>错误约定（业务侧重试参考）：
 * <ul>
 *   <li>{@code 410 Gone}：事务已终止/超时，禁止重试，业务侧需重新发起 INVITE</li>
 *   <li>{@code 502 Bad Gateway}：nodeAddressMap 暂未刷新到目标节点，建议 200ms × 3 短重试</li>
 *   <li>{@code 503 Service Unavailable}：目标节点转发失败 / store 后端不可达，建议短重试</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping("/gateway/gb28181")
@RequiredArgsConstructor
public class Gb28181InviteResponseController {

    private final GatewayProperties coreProps;
    private final ServerCommandSender commandSender;
    private final InviteContextStore inviteContextStore;
    @Qualifier("gatewayForwardRestTemplate")
    private final RestTemplate restTemplate;

    @PostMapping("/invite/response")
    public void inviteResponse(@RequestBody InviteResponseRequest req) {
        InviteContext ctx = findContextOrTranslate(req.getCallId());
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "invite ctx expired or unknown callId");
        }

        if (coreProps.getNodeId().equals(ctx.nodeId())) {
            handleLocally(ctx.ctxKey(), req);
            safeRemove(req.getCallId());
            return;
        }

        // 跨节点转发
        String targetUrl = coreProps.getNodes().get(ctx.nodeId());
        if (targetUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "unknown node: " + ctx.nodeId());
        }
        try {
            restTemplate.postForObject(targetUrl + "/gateway/gb28181/invite/response", req, Void.class);
        } catch (RestClientException e) {
            log.warn("跨节点 INVITE 回包失败: targetNode={}, callId={}", ctx.nodeId(), req.getCallId(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "forward failed: " + e.getMessage(), e);
        }
    }

    private void handleLocally(String ctxKey, InviteResponseRequest req) {
        TransactionContextInfo ctx = SipTransactionRegistry.getContext(ctxKey);
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "transaction terminated");
        }
        int statusCode = req.getStatusCode() != null ? req.getStatusCode() : Response.OK;
        ResponseCmd.sendResponse(statusCode, req.getSdp(),
                ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(),
                ctx.getOriginalEvent());
    }

    private InviteContext findContextOrTranslate(String callId) {
        try {
            return inviteContextStore.find(callId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("InviteContextStore.find 失败: callId={}", callId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "invite ctx store unavailable: " + e.getMessage(), e);
        }
    }

    private void safeRemove(String callId) {
        try {
            inviteContextStore.remove(callId);
        } catch (RuntimeException e) {
            log.warn("InviteContextStore.remove 失败（已成功回包，忽略）: callId={}", callId, e);
        }
    }
}
