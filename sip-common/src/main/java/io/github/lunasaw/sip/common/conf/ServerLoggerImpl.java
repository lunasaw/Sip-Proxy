package io.github.lunasaw.sip.common.conf;

import java.util.Properties;

import javax.sip.SipStack;

import gov.nist.core.CommonLogger;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.stack.SIPTransactionStack;

/**
 * JAIN-SIP ServerLogger 实现，将SIP消息日志输出到框架日志系统。
 */
public class ServerLoggerImpl implements ServerLogger {

    protected StackLogger stackLogger;
    private boolean showLog = true;
    private SIPTransactionStack sipStack;

    @Override
    public void closeLogFile() {

    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, boolean sender, long time) {
        if (!showLog) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sender ? from + "发送：目标--->" + to : to + " 接收：来自--->" + from)
            .append("\r\n")
                .append(message);
        this.stackLogger.logInfo(stringBuilder.toString());

    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender, long time) {

    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender) {

    }

    @Override
    public void logException(Exception ex) {
        if (!showLog) {
            return;
        }
        this.stackLogger.logException(ex);
    }

    @Override
    public void setStackProperties(Properties stackProperties) {
        if (!showLog) {
            return;
        }
        String TRACE_LEVEL = stackProperties.getProperty("gov.nist.javax.sip.TRACE_LEVEL");
        if (TRACE_LEVEL != null) {
            showLog = true;
        }
    }

    @Override
    public void setSipStack(SipStack sipStack) {
        if (!showLog) {
            return;
        }
        if (sipStack instanceof SIPTransactionStack) {
            this.sipStack = (SIPTransactionStack) sipStack;
            this.stackLogger = CommonLogger.getLogger(SIPTransactionStack.class);
        }
    }
}
