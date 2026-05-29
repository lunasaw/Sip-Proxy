package io.github.lunasaw.sip.common.entity;

import java.util.List;

import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.message.Request;

import org.apache.commons.collections4.CollectionUtils;
import java.util.ArrayList;

import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.sip.common.enums.ContentTypeEnum;
import io.github.lunasaw.sip.common.sequence.GenerateSequenceImpl;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import lombok.Data;

/**
 * SIP消息体，封装构建一次SIP请求或响应所需的全部字段。
 */
@Data
public class SipMessage {

    /**
     * 单次请求唯一标识 可以使用时间戳 自增即可
     */
    private Long              sequence;

    /**
     * 事物响应唯一标识 作为同一个请求判断
     */
    private String            callId;

    /**
     * sip请求 方式
     */
    private String            method;

    /**
     * 单次sip请求唯一标识
     * viaTag用于标识SIP消息的唯一性，每个SIP消息都应该包含一个viaTag字段，这个字段的值是由发送方生成的随机字符串，用于标识该消息的唯一性。在SIP消息的传输过程中，每个中间节点都会将viaTag字段的值更新为自己生成的随机字符串，以确保消息的唯一性。
     */
    private String            viaTag;

    /**
     * sip请求 内容
     */
    private String            content;

    /**
     * sip请求 请求类型
     */
    private ContentTypeHeader contentTypeHeader;

    /**
     * 自定义header
     */
    private List<Header>      headers;

    /**
     * 响应状态码
     */
    private Integer statusCode;

    /**
     * 构建 MESSAGE 请求消息体，内容类型为 Application/MANSCDP+xml。
     *
     * @return SipMessage实例
     */
    public static SipMessage getMessageBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.MESSAGE);
        sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_XML.getContentTypeHeader());
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 INVITE 请求消息体，内容类型为 APPLICATION/SDP。
     *
     * @return SipMessage实例
     */
    public static SipMessage getInviteBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.INVITE);
        sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader());
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 BYE 请求消息体。
     *
     * @return SipMessage实例
     */
    public static SipMessage getByeBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.BYE);
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 SUBSCRIBE 请求消息体，内容类型为 Application/MANSCDP+xml。
     *
     * @return SipMessage实例
     */
    public static SipMessage getSubscribeBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.SUBSCRIBE);
        sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_XML.getContentTypeHeader());
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 INFO 请求消息体，内容类型为 Application/MANSRTSP。
     *
     * @return SipMessage实例
     */
    public static SipMessage getInfoBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.INFO);
        sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_MAN_SRTSP.getContentTypeHeader());
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 NOTIFY 请求消息体，内容类型为 Application/MANSCDP+xml。
     *
     * @return SipMessage实例
     */
    public static SipMessage getNotifyBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.NOTIFY);
        sipMessage.setContentTypeHeader(ContentTypeEnum.APPLICATION_XML.getContentTypeHeader());
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 基于 SIPResponse 构建 ACK 请求消息体，序号取自响应的 CSeq。
     *
     * @param sipResponse SIP响应
     * @return SipMessage实例
     */
    public static SipMessage getAckBody(SIPResponse sipResponse) {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.ACK);
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        sipMessage.setSequence(sipResponse.getCSeqHeader().getSeqNumber());

        return sipMessage;
    }

    /**
     * 构建 ACK 请求消息体，自动生成序号。
     *
     * @return SipMessage实例
     */
    public static SipMessage getAckBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.ACK);
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建 REGISTER 请求消息体。
     *
     * @return SipMessage实例
     */
    public static SipMessage getRegisterBody() {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setMethod(Request.REGISTER);
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 构建响应消息体。
     *
     * @param statusCode HTTP状态码
     * @return SipMessage实例
     */
    public static SipMessage getResponse(int statusCode) {
        SipMessage sipMessage = new SipMessage();
        sipMessage.setViaTag(SipRequestUtils.getNewViaTag());
        long sequence = GenerateSequenceImpl.getSequence();
        sipMessage.setStatusCode(statusCode);
        sipMessage.setSequence(sequence);

        return sipMessage;
    }

    /**
     * 向消息头列表追加一个自定义头。
     *
     * @param header 要追加的头
     * @return 当前SipMessage实例（支持链式调用）
     */
    public SipMessage addHeader(Header header) {
        if (CollectionUtils.isEmpty(headers)) {
            headers = new ArrayList<>(java.util.Arrays.asList(header));
        } else {
            headers.add(header);
        }
        return this;
    }

}
