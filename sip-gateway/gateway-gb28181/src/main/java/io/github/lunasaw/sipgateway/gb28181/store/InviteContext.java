package io.github.lunasaw.sipgateway.gb28181.store;

/**
 * INVITE 上下文路由二元组：标识接收 INVITE 的节点 + 该节点上 SipTransactionRegistry 的上下文键。
 *
 * @param nodeId 收到 INVITE 的节点 ID
 * @param ctxKey SipTransactionRegistry 中的上下文键（callId_fromTag_cseq）
 * @author luna
 */
public record InviteContext(String nodeId, String ctxKey) {
}
