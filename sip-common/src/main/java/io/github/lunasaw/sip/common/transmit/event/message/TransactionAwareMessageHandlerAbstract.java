package io.github.lunasaw.sip.common.transmit.event.message;

import com.luna.common.text.StringTools;
import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.transmit.TransactionAwareResponseCmd;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry;
import io.github.lunasaw.sip.common.transmit.SipTransactionRegistry.TransactionContextInfo;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.message.Response;
import java.nio.charset.Charset;

/**
 * 事务感知的消息处理器抽象基类
 * 继承原有MessageHandlerAbstract，增强事务管理能力
 *
 * @author weidian
 * @author luna (enhanced)
 */
@Getter
@Setter
@Slf4j
public abstract class TransactionAwareMessageHandlerAbstract implements MessageHandler, InitializingBean {

    private String xmlStr;

    /**
     * 解析请求内容为对象（静态方法保持兼容）
     */
    public static <T> T parseRequest(RequestEvent event, String charset, Class<T> clazz) {
        SIPRequest sipRequest = (SIPRequest) event.getRequest();
        byte[] rawContent = sipRequest.getRawContent();
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        String xmlStr = StringTools.toEncodedString(rawContent, Charset.forName(charset));
        Object o = XmlUtils.parseObj(xmlStr, clazz);
        return (T) o;
    }

    /**
     * 解析请求内容为字符串（静态方法保持兼容）
     */
    public static String parseRequest(RequestEvent event, String charset) {
        SIPRequest sipRequest = (SIPRequest) event.getRequest();
        byte[] rawContent = sipRequest.getRawContent();
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        return StringTools.toEncodedString(rawContent, Charset.forName(charset));
    }

    @Override
    public void afterPropertiesSet() {
        SipMessageRequestProcessorAbstract.addHandler(this);
    }

    @Override
    public void handForEvt(RequestEvent event) {
        // 子类实现具体处理逻辑
    }

    @Override
    public String getRootType() {
        return null;
    }

    public String getMethod() {
        return null;
    }

    @Override
    public String getCmdType() {
        return null;
    }

    @Override
    public void setXmlStr(String xmlStr) {
        this.xmlStr = xmlStr;
    }

    /**
     * 获取设备会话（子类可重写）
     */
    public DeviceSession getDeviceSession(RequestEvent event) {
        return null;
    }

    // ==================== 事务感知的响应方法 ====================

    /**
     * 事务感知的ACK响应（优先使用事务上下文）
     */
    public void responseAck(RequestEvent event) {
        try {
            // 优先使用事务感知方式
            if (TransactionAwareResponseCmd.hasValidTransactionContext()) {
                TransactionAwareResponseCmd.sendOK();
                log.debug("使用事务上下文发送OK响应成功");
                return;
            }

            // 降级到原有方式
            log.debug("未找到有效事务上下文，使用原有方式发送OK响应");
            ResponseCmd.sendResponse(Response.OK, "OK", event);

        } catch (Exception e) {
            log.error("发送OK响应失败，尝试无事务模式", e);
            try {
                ResponseCmd.sendResponseNoTransaction(Response.OK, "OK", event);
            } catch (Exception fallbackException) {
                log.error("无事务模式发送OK响应也失败", fallbackException);
                throw new RuntimeException("发送ACK响应失败", e);
            }
        }
    }

    /**
     * 事务感知的ACK响应（使用预创建的事务）
     */
    @Override
    public void responseAck(RequestEvent event, ServerTransaction serverTransaction) {
        try {
            // 优先使用事务感知方式
            if (TransactionAwareResponseCmd.hasValidTransactionContext()) {
                TransactionAwareResponseCmd.sendOK();
                log.debug("使用事务上下文发送OK响应成功（忽略传入的serverTransaction）");
                return;
            }

            // 使用传入的事务
            if (serverTransaction != null) {
                ResponseCmd.sendResponse(Response.OK, "OK", event, serverTransaction);
                log.debug("使用传入的ServerTransaction发送OK响应成功");
                return;
            }

            // 降级到无事务方式
            log.debug("未找到有效事务，使用无事务方式发送OK响应");
            responseAck(event);

        } catch (Exception e) {
            log.error("发送OK响应失败: {}", e.getMessage(), e);
            // 最后的降级尝试
            try {
                ResponseCmd.sendResponseNoTransaction(Response.OK, "OK", event);
                log.debug("使用无事务模式成功发送OK响应");
            } catch (Exception fallbackException) {
                log.error("无事务模式发送OK响应也失败", fallbackException);
                throw new RuntimeException("发送ACK响应失败", e);
            }
        }
    }

    /**
     * 事务感知的错误响应
     */
    public void responseError(RequestEvent event) {
        try {
            // 优先使用事务感知方式
            if (TransactionAwareResponseCmd.hasValidTransactionContext()) {
                TransactionAwareResponseCmd.sendInternalServerError();
                log.debug("使用事务上下文发送服务器错误响应成功");
                return;
            }

            // 降级到原有方式
            log.debug("未找到有效事务上下文，使用原有方式发送服务器错误响应");
            ResponseCmd.sendResponse(Response.SERVER_INTERNAL_ERROR, "SERVER ERROR", event);

        } catch (Exception e) {
            log.error("发送服务器错误响应失败", e);
            try {
                ResponseCmd.sendResponseNoTransaction(Response.SERVER_INTERNAL_ERROR, "SERVER ERROR", event);
            } catch (Exception fallbackException) {
                log.error("无事务模式发送服务器错误响应也失败", fallbackException);
                throw new RuntimeException("发送错误响应失败", e);
            }
        }
    }

    /**
     * 事务感知的自定义错误响应
     */
    public void responseError(RequestEvent event, Integer code, String error) {
        try {
            // 优先使用事务感知方式
            if (TransactionAwareResponseCmd.hasValidTransactionContext()) {
                TransactionAwareResponseCmd.sendResponse(code, error);
                log.debug("使用事务上下文发送自定义错误响应成功: code={}, error={}", code, error);
                return;
            }

            // 降级到原有方式
            log.debug("未找到有效事务上下文，使用原有方式发送自定义错误响应: code={}, error={}", code, error);
            ResponseCmd.sendResponse(code, error, event);

        } catch (Exception e) {
            log.error("发送自定义错误响应失败: code={}, error={}", code, error, e);
            try {
                ResponseCmd.sendResponseNoTransaction(code, error, event);
            } catch (Exception fallbackException) {
                log.error("无事务模式发送自定义错误响应也失败", fallbackException);
                throw new RuntimeException("发送自定义错误响应失败", e);
            }
        }
    }

    // ==================== 事务上下文辅助方法 ====================

    /**
     * 获取当前事务上下文信息
     */
    protected TransactionContextInfo getCurrentTransactionContext() {
        return SipTransactionRegistry.getCurrentContext();
    }

    /**
     * 检查当前是否有有效的事务上下文
     */
    protected boolean hasValidTransactionContext() {
        return TransactionAwareResponseCmd.hasValidTransactionContext();
    }

    /**
     * 获取当前事务上下文的详细信息（用于日志和调试）
     */
    protected String getCurrentTransactionInfo() {
        return TransactionAwareResponseCmd.getCurrentTransactionInfo();
    }

    /**
     * 记录事务上下文状态（用于调试）
     */
    protected void logTransactionContextStatus(String operation) {
        if (log.isDebugEnabled()) {
            TransactionContextInfo context = getCurrentTransactionContext();
            if (context != null) {
                log.debug("操作: {}, 事务上下文: key={}, 有效性={}, 状态={}",
                        operation,
                        context.getContextKey(),
                        context.isValid(),
                        context.getLastKnownState());
            } else {
                log.debug("操作: {}, 无事务上下文", operation);
            }
        }
    }

    /**
     * 安全执行需要事务上下文的操作
     */
    protected void executeWithTransactionContext(String operation, Runnable action) {
        logTransactionContextStatus("开始-" + operation);
        try {
            action.run();
            logTransactionContextStatus("完成-" + operation);
        } catch (Exception e) {
            logTransactionContextStatus("失败-" + operation);
            log.error("执行事务感知操作失败: {}", operation, e);
            throw e;
        }
    }

    /**
     * 事务感知的消息处理模板方法
     * 子类可以重写此方法来利用事务上下文进行处理
     */
    protected void handleWithTransactionContext(RequestEvent event, String operation) {
        executeWithTransactionContext(operation, () -> {
            try {
                // 调用子类的具体处理逻辑
                doHandleWithContext(event);

                // 自动发送ACK响应
                responseAck(event);

            } catch (Exception e) {
                log.error("事务感知消息处理失败: operation={}", operation, e);
                responseError(event);
                throw e;
            }
        });
    }

    /**
     * 子类实现的具体处理逻辑（在事务上下文中执行）
     * 子类可以重写此方法来实现具体的业务逻辑
     */
    protected void doHandleWithContext(RequestEvent event) {
        // 默认调用原有的处理方法
        handForEvt(event);
    }

    // ==================== 调试和监控方法 ====================

    /**
     * 获取处理器状态信息
     */
    public String getHandlerStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("处理器类型: ").append(this.getClass().getSimpleName()).append("\n");
        info.append("根类型: ").append(getRootType()).append("\n");
        info.append("命令类型: ").append(getCmdType()).append("\n");
        info.append("方法: ").append(getMethod()).append("\n");
        info.append("当前事务状态: ").append(getCurrentTransactionInfo()).append("\n");
        return info.toString();
    }
}