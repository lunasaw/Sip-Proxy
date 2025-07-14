package io.github.lunasaw.sip.common.layer;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.*;

import lombok.Setter;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import io.github.lunasaw.sip.common.conf.DefaultProperties;
import io.github.lunasaw.sip.common.conf.msg.StringMsgParserFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * SIP协议层封装
 * 提供SIP协议栈的初始化和监听点管理
 *
 * @author luna
 */
@Setter
@Slf4j
@Component
public class SipLayer implements InitializingBean {

    private static final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
    private static final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();

    // 记录已创建的监听点，避免重复创建
    private static final Map<String, Boolean>         listeningPoints   = new ConcurrentHashMap<>();

    // 记录SipStack实例，避免重复创建
    private static final Map<String, SipStackImpl>    sipStackMap       = new ConcurrentHashMap<>();

    @Getter
    private static final List<String>                 monitorIpList     = Lists.newArrayList("0.0.0.0");

    @Getter
    private SipListener sipListener;


    public static SipProviderImpl getUdpSipProvider(String ip) {
        if (ObjectUtils.isEmpty(ip)) {
            return null;
        }
        return udpSipProviderMap.get(ip);
    }

    public static SipProviderImpl getUdpSipProvider() {
        if (udpSipProviderMap.isEmpty()) {
            throw new RuntimeException("ListeningPoint Not Exist");
        }
        return udpSipProviderMap.values().stream().findFirst().get();
    }

    public static SipProviderImpl getTcpSipProvider() {
        if (tcpSipProviderMap.size() != 1) {
            return null;
        }
        return tcpSipProviderMap.values().stream().findFirst().get();
    }

    public static SipProviderImpl getTcpSipProvider(String ip) {
        if (ObjectUtils.isEmpty(ip)) {
            return null;
        }
        return tcpSipProviderMap.get(ip);
    }

    public static String getMonitorIp() {
        return monitorIpList.get(0);
    }

    /**
     * 生成监听点唯一标识
     */
    private String getListeningPointKey(String monitorIp, int port) {
        return monitorIp + ":" + port;
    }

    /**
     * 检查监听点是否已存在
     */
    private boolean isListeningPointExists(String monitorIp, int port) {
        String key = getListeningPointKey(monitorIp, port);
        return listeningPoints.containsKey(key);
    }

    /**
     * 标记监听点已创建
     */
    private void markListeningPointCreated(String monitorIp, int port) {
        String key = getListeningPointKey(monitorIp, port);
        listeningPoints.put(key, true);
    }

    /**
     * 添加监听点（简化版本）
     */
    public void addListeningPoint(String monitorIp, int port) {
        addListeningPoint(monitorIp, port, sipListener, true);
    }

    /**
     * 添加监听点（带日志控制）
     */
    public void addListeningPoint(String monitorIp, int port, Boolean enableLog) {
        addListeningPoint(monitorIp, port, sipListener, enableLog);
    }

    /**
     * 添加监听点（完整版本）
     * 优化：避免重复创建相同IP和端口的监听点
     */
    public synchronized void addListeningPoint(String monitorIp, int port, SipListener listener, Boolean enableLog) {
        // 检查是否已存在相同的监听点
        if (isListeningPointExists(monitorIp, port)) {
            log.info("[SIP SERVER] 监听点 {}:{} 已存在，跳过创建", monitorIp, port);
            return;
        }

        SipStackImpl sipStack = getOrCreateSipStack(monitorIp, enableLog);
        if (sipStack == null) {
            return;
        }

        boolean tcpSuccess = createTcpListeningPoint(sipStack, monitorIp, port, listener);
        boolean udpSuccess = createUdpListeningPoint(sipStack, monitorIp, port, listener);

        // 只有当TCP或UDP至少有一个成功时，才标记为已创建
        if (tcpSuccess || udpSuccess) {
            markListeningPointCreated(monitorIp, port);
            if (!monitorIpList.contains(monitorIp)) {
                monitorIpList.add(monitorIp);
            }
        }
    }

    /**
     * 获取或创建SipStack实例
     */
    private SipStackImpl getOrCreateSipStack(String monitorIp, Boolean enableLog) {
        String stackKey = monitorIp + "_" + (enableLog ? "log" : "nolog");

        if (sipStackMap.containsKey(stackKey)) {
            return sipStackMap.get(stackKey);
        }

        try {
            Properties properties = DefaultProperties.getProperties("SIP-PROXY", monitorIp, enableLog);
            SipFactory sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            SipStackImpl sipStack = (SipStackImpl)sipFactory.createSipStack(properties);
            sipStack.setMessageParserFactory(new StringMsgParserFactory());

            sipStackMap.put(stackKey, sipStack);
            log.info("[SIP SERVER] 创建新的SipStack实例: {}", stackKey);
            return sipStack;
        } catch (PeerUnavailableException e) {
            log.error("[SIP SERVER] SIP服务启动失败，监听地址{}失败,请检查ip是否正确", monitorIp, e);
            return null;
        }
    }

    /**
     * 创建TCP监听点
     */
    private boolean createTcpListeningPoint(SipStackImpl sipStack, String monitorIp, int port, SipListener listener) {
        try {
            ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
            SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);

            tcpSipProvider.setDialogErrorsAutomaticallyHandled();
            tcpSipProvider.addSipListener(listener);
            tcpSipProviderMap.put(monitorIp, tcpSipProvider);
            log.info("[SIP SERVER] tcp://{}:{} 启动成功", monitorIp, port);
            return true;
        } catch (TransportNotSupportedException
            | TooManyListenersException
            | ObjectInUseException
            | InvalidArgumentException e) {
            log.error("[SIP SERVER] tcp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确", monitorIp, port, e);
            return false;
        }
    }

    /**
     * 创建UDP监听点
     */
    private boolean createUdpListeningPoint(SipStackImpl sipStack, String monitorIp, int port, SipListener listener) {
        try {
            ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");
            SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
            udpSipProvider.addSipListener(listener);

            udpSipProviderMap.put(monitorIp, udpSipProvider);
            log.info("[SIP SERVER] udp://{}:{} 启动成功", monitorIp, port);
            return true;
        } catch (TransportNotSupportedException
            | TooManyListenersException
            | ObjectInUseException
            | InvalidArgumentException e) {
            log.error("[SIP SERVER] udp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确", monitorIp, port, e);
            return false;
        }
    }

    /**
     * 清理指定IP和端口的监听点
     */
    public synchronized void removeListeningPoint(String monitorIp, int port) {
        String key = getListeningPointKey(monitorIp, port);

        // 清理TCP Provider
        SipProviderImpl tcpProvider = tcpSipProviderMap.remove(monitorIp);
        if (tcpProvider != null) {
            try {
                tcpProvider.removeSipListener(sipListener);
                log.info("[SIP SERVER] 清理TCP监听点: {}:{}", monitorIp, port);
            } catch (Exception e) {
                log.warn("[SIP SERVER] 清理TCP监听点时发生异常: {}", e.getMessage());
            }
        }

        // 清理UDP Provider
        SipProviderImpl udpProvider = udpSipProviderMap.remove(monitorIp);
        if (udpProvider != null) {
            try {
                udpProvider.removeSipListener(sipListener);
                log.info("[SIP SERVER] 清理UDP监听点: {}:{}", monitorIp, port);
            } catch (Exception e) {
                log.warn("[SIP SERVER] 清理UDP监听点时发生异常: {}", e.getMessage());
            }
        }

        // 移除监听点记录
        listeningPoints.remove(key);
        monitorIpList.remove(monitorIp);
    }

    /**
     * 清理所有监听点
     */
    public synchronized void clearAllListeningPoints() {
        log.info("[SIP SERVER] 开始清理所有监听点");

        // 清理所有TCP Provider
        tcpSipProviderMap.clear();

        // 清理所有UDP Provider
        udpSipProviderMap.clear();

        // 清理监听点记录
        listeningPoints.clear();
        monitorIpList.clear();
        monitorIpList.add("0.0.0.0"); // 恢复默认值

        // 清理SipStack实例
        sipStackMap.clear();

        log.info("[SIP SERVER] 所有监听点清理完成");
    }

    /**
     * 获取当前活跃的监听点数量
     */
    public int getActiveListeningPointsCount() {
        return listeningPoints.size();
    }

    /**
     * 检查指定IP和端口是否有活跃的监听点
     */
    public boolean hasActiveListeningPoint(String monitorIp, int port) {
        return isListeningPointExists(monitorIp, port);
    }

    public String getLocalIp(String deviceLocalIp) {
        if (!ObjectUtils.isEmpty(deviceLocalIp)) {
            return deviceLocalIp;
        }
        return getUdpSipProvider().getListeningPoint().getIPAddress();
    }

    @Override
    public void afterPropertiesSet() {

    }
}