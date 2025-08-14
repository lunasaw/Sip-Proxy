package io.github.lunasaw.sip.common.transmit;

import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.layer.SipLayer;
import lombok.extern.slf4j.Slf4j;

import javax.sip.ServerTransaction;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import java.util.Objects;

/**
 * SIP事务管理器
 * 负责SIP事务的创建和管理
 *
 * @author lin
 */
@Slf4j
public class SipTransactionManager {

    /**
     * 获取服务器事务
     *
     * @param request 请求
     * @return 服务器事务
     */
    public static ServerTransaction getServerTransaction(Request request) {
        return getServerTransaction(request, SipLayer.getMonitorIp());
    }

    /**
     * 根据请求获取服务器事务
     *
     * @param request 请求
     * @param ip 监听IP
     * @return 服务器事务
     */
    public static ServerTransaction getServerTransaction(Request request, String ip) {
        // 检查是否正在关闭
        if (SipLayer.isShuttingDown()) {
            log.debug("服务正在关闭，跳过事务创建");
            return null;
        }
        
        if (ip == null) {
            ip = SipLayer.getMonitorIp();
        }

        String transport = getTransport(request);
        boolean isTcp = Constant.TCP.equalsIgnoreCase(transport);

        try {
            ServerTransaction serverTransaction = null;
            if (isTcp) {
                SipProviderImpl sipProvider = SipLayer.getTcpSipProvider(ip);
                if (sipProvider == null) {
                    log.warn("[发送信息失败] 未找到tcp://{}的监听信息，可能服务正在关闭", ip);
                    return null;
                }
                SipStackImpl stack = (SipStackImpl)sipProvider.getSipStack();
                serverTransaction = (SIPServerTransaction)stack.findTransaction((SIPRequest)request, true);
                if (serverTransaction == null) {
                    serverTransaction = sipProvider.getNewServerTransaction(request);
                }
            } else {
                SipProviderImpl udpSipProvider = SipLayer.getUdpSipProvider(ip);
                if (udpSipProvider == null) {
                    log.warn("[发送信息失败] 未找到udp://{}的监听信息，可能服务正在关闭", ip);
                    return null;
                }
                SipStackImpl stack = (SipStackImpl)udpSipProvider.getSipStack();
                serverTransaction = (SIPServerTransaction)stack.findTransaction((SIPRequest)request, true);
                if (serverTransaction == null) {
                    serverTransaction = udpSipProvider.getNewServerTransaction(request);
                }
            }
            return serverTransaction;
        } catch (Exception e) {
            log.error("获取服务器事务失败", e);
            // 返回null而不是抛出异常，让调用者处理
            return null;
        }
    }

    /**
     * 获取传输协议
     *
     * @param request 请求
     * @return 传输协议
     */
    private static String getTransport(Request request) {
        ViaHeader viaHeader = (ViaHeader)request.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[消息头缺失]： ViaHeader， 使用默认的UDP方式处理数据");
        } else {
            transport = viaHeader.getTransport();
        }
        return transport;
    }
}