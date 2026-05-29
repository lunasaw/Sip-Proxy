package io.github.lunasaw.sip.common.conf.msg;

import gov.nist.javax.sip.parser.MessageParser;
import gov.nist.javax.sip.parser.MessageParserFactory;
import gov.nist.javax.sip.stack.SIPTransactionStack;

/**
 * JAIN-SIP MessageParserFactory 实现，提供无状态的 StringMsgParser 单例。
 */
public class StringMsgParserFactory implements MessageParserFactory {

    /**
     * msg parser is completely stateless, reuse isntance for the whole stack
     * fixes https://github.com/RestComm/jain-sip/issues/92
     */
    private static StringMsgParser msgParser = new StringMsgParser();

    /*
     * (non-Javadoc)
     * @see gov.nist.javax.sip.parser.MessageParserFactory#createMessageParser(gov.nist.javax.sip.stack.SIPTransactionStack)
     */
    /**
     * 创建消息解析器，返回共享的无状态单例。
     *
     * @param stack SIP事务栈
     * @return 消息解析器实例
     */
    public MessageParser createMessageParser(SIPTransactionStack stack) {
        return msgParser;
    }
}
