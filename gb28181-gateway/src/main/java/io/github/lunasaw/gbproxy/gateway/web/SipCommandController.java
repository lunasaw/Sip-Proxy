package io.github.lunasaw.gbproxy.gateway.web;

import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore.InviteContext;
import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
import io.github.lunasaw.gbproxy.gateway.dto.*;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sip.message.Response;
import java.util.Map;

/**
 * sip-gateway HTTP API，对应 LAYERED-ARCHITECTURE.md §6.4。
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
@RequestMapping("/sip")
@RequiredArgsConstructor
public class SipCommandController {

    private final GatewayProperties gatewayProperties;
    private final ServerCommandSender commandSender;
    private final InviteContextStore inviteContextStore;
    @Qualifier("gatewayForwardRestTemplate")
    private final RestTemplate restTemplate;

    @PostMapping("/invite/start")
    public Map<String, String> invitePlay(@RequestBody InviteStartRequest req) {
        String callId = commandSender.deviceInvitePlay(req.getDeviceId(),
                req.getMediaIp(), req.getMediaPort(), req.getStreamMode());
        return Map.of("callId", callId);
    }

    @PostMapping("/invite/bye")
    public void bye(@RequestBody ByeRequest req) {
        commandSender.deviceBye(req.getCallId());
    }

    @PostMapping("/invite/response")
    public void inviteResponse(@RequestBody InviteResponseRequest req) {
        InviteContext ctx = findContextOrTranslate(req.getCallId());
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "invite ctx expired or unknown callId");
        }

        if (gatewayProperties.getNodeId().equals(ctx.nodeId())) {
            handleLocally(ctx.ctxKey(), req);
            safeRemove(req.getCallId());
            return;
        }

        // 跨节点转发
        String targetUrl = gatewayProperties.getNodes().get(ctx.nodeId());
        if (targetUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "unknown node: " + ctx.nodeId());
        }
        try {
            restTemplate.postForObject(targetUrl + "/sip/invite/response", req, Void.class);
        } catch (RestClientException e) {
            log.warn("跨节点 INVITE 回包失败: targetNode={}, callId={}", ctx.nodeId(), req.getCallId(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "forward failed: " + e.getMessage(), e);
        }
    }

    @PostMapping("/control/ptz")
    public void ptz(@RequestBody PtzRequest req) {
        commandSender.deviceControlPtzCmd(req.getDeviceId(), req.getCmd(), req.getSpeed());
    }

    @PostMapping("/query/catalog")
    public Map<String, String> catalog(@RequestBody CatalogQueryRequest req) {
        String sn = commandSender.deviceCatalogQuery(req.getDeviceId());
        return Map.of("sn", sn);
    }

    @PostMapping("/whoami")
    public ResponseEntity<Map<String, String>> whoami() {
        return ResponseEntity.ok(Map.of("nodeId", gatewayProperties.getNodeId()));
    }

    private void handleLocally(String ctxKey, InviteResponseRequest req) {
        TransactionContextInfo ctx = SipTransactionRegistry.getContext(ctxKey);
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "transaction terminated");
        }
        ResponseCmd.sendResponse(Response.OK, req.getSdp(),
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
