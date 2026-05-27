package io.github.lunasaw.sip.common.entity;

import lombok.Data;

/**
 * SIP事务交换信息，包含标识一次SIP事务所需的关键字段。
 */
@Data
public class SipTransaction {

    /** 会话唯一标识 Call-ID。 */
    private String callId;
    /** 发送方标签 From-Tag。 */
    private String fromTag;
    /** 接收方标签 To-Tag。 */
    private String toTag;
    /** Via头域分支参数，用于事务匹配。 */
    private String viaBranch;


}
