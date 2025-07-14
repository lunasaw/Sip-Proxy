package io.github.lunasaw.sip.common.transmit.event.message;

import java.nio.charset.Charset;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import org.apache.commons.lang3.StringUtils;

import com.luna.common.text.StringTools;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import io.github.lunasaw.sip.common.transmit.ResponseCmd;
import io.github.lunasaw.sip.common.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * @author weidian
 */
@Getter
@Setter
public abstract class MessageHandlerAbstract implements MessageHandler {

    private String xmlStr;

    public static <T> T parseRequest(RequestEvent event, String charset, Class<T> clazz) {
        SIPRequest sipRequest = (SIPRequest)event.getRequest();
        byte[] rawContent = sipRequest.getRawContent();
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        String xmlStr = StringTools.toEncodedString(rawContent, Charset.forName(charset));
        Object o = XmlUtils.parseObj(xmlStr, clazz);
        return (T)o;
    }

    public static String parseRequest(RequestEvent event, String charset) {
        SIPRequest sipRequest = (SIPRequest)event.getRequest();
        byte[] rawContent = sipRequest.getRawContent();
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        return StringTools.toEncodedString(rawContent, Charset.forName(charset));
    }

    @Override
    public void handForEvt(RequestEvent event) {

    }

    @Override
    public String getRootType() {
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

    public DeviceSession getDeviceSession(RequestEvent event) {

        return null;
    }

    public void responseAck(RequestEvent event) {
        ResponseCmd.doResponseCmd(Response.OK, "OK", event);
    }

    public void responseError(RequestEvent event) {
        ResponseCmd.doResponseCmd(Response.SERVER_INTERNAL_ERROR, "SERVER ERROR", event);
    }

    public void responseError(RequestEvent event, Integer code, String error) {
        ResponseCmd.doResponseCmd(code, error, event);
    }

    public <T> T parseXml(Class<T> clazz) {
        if (StringUtils.isBlank(xmlStr)) {
            return null;
        }
        return (T)XmlUtils.parseObj(xmlStr, clazz);
    }
}
