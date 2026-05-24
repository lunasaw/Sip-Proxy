package io.github.lunasaw.gbproxy.test.gateway;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.gbproxy.test.gateway.dto.ByeRequest;
import io.github.lunasaw.gbproxy.test.gateway.dto.CatalogQueryRequest;
import io.github.lunasaw.gbproxy.test.gateway.dto.InviteResponseRequest;
import io.github.lunasaw.gbproxy.test.gateway.dto.InviteStartRequest;
import io.github.lunasaw.gbproxy.test.gateway.dto.PtzRequest;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sip.message.Response;
import java.util.Map;

/**
 * sip-gateway HTTP API 实现，对应 LAYERED-ARCHITECTURE.md §6.4。
 *
 * <p>暴露给业务服务器调用：
 * <ul>
 *   <li>{@code POST /sip/invite/start}：平台主动 INVITE</li>
 *   <li>{@code POST /sip/invite/bye}：终止已建立的 INVITE 会话</li>
 *   <li>{@code POST /sip/invite/response}：设备主��� INVITE 时业务侧异步回包，自动跨节点路由</li>
 *   <li>{@code POST /sip/control/ptz}：PTZ 控制</li>
 *   <li>{@code POST /sip/query/catalog}：目录查询（响应通过 {@code DeviceCatalogEvent} 异步返回）</li>
 * </ul>
 *
 * <p>错误约定（业务侧重试参考，与文档 §6.4 注脚一致）：
 * <ul>
 *   <li>{@code 410 Gone}：事务已终止/超时，禁止重试，业务侧需重新发起 INVITE</li>
 *   <li>{@code 502 Bad Gateway}：nodeAddressMap 暂未刷新到目标节点，建议 200ms × 3 短重试</li>
 *   <li>{@code 503 Service Unavailable}：目标节点转发失败（含 Redis/���络抖动），建议短重试</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/sip")
@RequiredArgsConstructor
public class SipCommandController {

    private final GatewayProperties gatewayProperties;
    private final ServerCommandSender commandSender;
    private final InviteContextStore inviteContextStore;
    /** 单机部署可不注入 RestTemplate，find() 永远落本节点 */
    private final ObjectProvider<RestTemplate> restTemplateProvider;

    @PostMapping("/invite/start")
    public Map<String, String> invitePlay(@RequestBody InviteStartRequest req) {
        String callId = commandSender.deviceInvitePlay(req.getDeviceId(),
                req.getMediaIp(), req.getMediaPort(), req.getStreamMode());
        return Map.of("callId", callId);
    }

    @PostMapping("/invite/bye")
    public void bye(@RequestBody ByeRequest req) {
        commandSender.deviceBye(req.getDeviceId(), req.getCallId());
    }

    @PostMapping("/invite/response")
    public void inviteResponse(@RequestBody InviteResponseRequest req) {
        String value = inviteContextStore.find(req.getCallId());
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "invite ctx expired or unknown callId");
        }

        int sep = value.indexOf(':');
        if (sep < 0) {
            log.warn("invite ctx 格式异常: {}", value);
            throw new ResponseStatusException(HttpStatus.GONE, "invite ctx malformed");
        }
        String targetNode = value.substring(0, sep);
        String ctxKey = value.substring(sep + 1);

        if (gatewayProperties.getNodeId().equals(targetNode)) {
            handleLocally(ctxKey, req);
            inviteContextStore.remove(req.getCallId());
            return;
        }

        // 跨节点转发
        String targetUrl = gatewayProperties.getNodes().get(targetNode);
        if (targetUrl == null) {
            // 节点上下线/IP 变更存在数秒刷新窗口
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "unknown node: " + targetNode);
        }
        RestTemplate restTemplate = restTemplateProvider.getIfAvailable();
        if (restTemplate == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "RestTemplate not configured; multi-node routing disabled");
        }
        try {
            restTemplate.postForObject(targetUrl + "/sip/invite/response", req, Void.class);
        } catch (RestClientException e) {
            log.warn("跨节点 INVITE 回包失败: targetNode={}, callId={}", targetNode, req.getCallId(), e);
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

    /** 健康探测：返回当前节点 ID，方便排查跨节点路由 */
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
}
