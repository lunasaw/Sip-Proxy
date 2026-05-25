package io.github.lunasaw.sip.common.utils;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.SubjectHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import com.luna.common.text.StringTools;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.sip.common.entity.RemoteAddressInfo;
import io.github.lunasaw.sip.common.entity.SipTransaction;

/**
 * @author luna
 */
public class SipUtils {

    // NTP时间戳偏移量 (1900年1月1日到1970年1月1日的秒数)
    private static final long NTP_TIMESTAMP_OFFSET = 2208988800L;

    public static String getUserIdFromToHeader(Response response) {
        ToHeader toHeader = (ToHeader)response.getHeader(ToHeader.NAME);
        return getUserIdFromHeader(toHeader);
    }

    public static String getUserIdFromFromHeader(Response response) {
        FromHeader fromHeader = (FromHeader)response.getHeader(FromHeader.NAME);
        return getUserIdFromHeader(fromHeader);
    }

    public static String getUserIdFromToHeader(Request request) {
        ToHeader toHeader = (ToHeader)request.getHeader(ToHeader.NAME);
        return getUserIdFromHeader(toHeader);
    }

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromHeader(fromHeader);
    }

    public static String getUser(Request request) {
        return ((SipUri)request.getRequestURI()).getUser();
    }

    public static SipTransaction getSipTransaction(SIPResponse response) {
        SipTransaction sipTransaction = new SipTransaction();
        sipTransaction.setCallId(response.getCallIdHeader().getCallId());
        sipTransaction.setFromTag(response.getFromTag());
        sipTransaction.setToTag(response.getToTag());
        sipTransaction.setViaBranch(response.getTopmostViaHeader().getBranch());
        return sipTransaction;
    }

    public static SipTransaction getSipTransaction(SIPRequest request) {
        SipTransaction sipTransaction = new SipTransaction();
        sipTransaction.setCallId(request.getCallIdHeader().getCallId());
        sipTransaction.setFromTag(request.getFromTag());
        sipTransaction.setToTag(request.getToTag());
        sipTransaction.setViaBranch(request.getTopmostViaHeader().getBranch());
        return sipTransaction;
    }

    public static String getUserIdFromHeader(HeaderAddress headerAddress) {
        AddressImpl address = (AddressImpl)headerAddress.getAddress();
        SipUri uri = (SipUri)address.getURI();
        return uri.getUser();
    }

    public static String getCallId(RequestEvent requestEvent) {
        return ((SIPRequest)requestEvent.getRequest()).getCallIdHeader().getCallId();
    }

    public static String getCallId(SIPRequest request) {
        return request.getCallIdHeader().getCallId();
    }

    public static RemoteAddressInfo getRemoteAddressFromRequest(SIPRequest request) {
        return getRemoteAddressFromRequest(request, false);
    }

    /**
     * 从subject读取channelId
     */
    public static String getSubjectId(Request request) {
        SubjectHeader subject = (SubjectHeader)request.getHeader(SubjectHeader.NAME);
        if (subject == null) {
            // 如果缺失subject
            return null;
        }
        return subject.getSubject().split(":")[0];
    }

    /**
     * 从请求中获取设备ip地址和端口号
     *
     * @param request 请求
     * @param sipUseSourceIpAsRemoteAddress false 从via中获取地址， true 直接获取远程地址
     * @return 地址信息
     */
    public static RemoteAddressInfo getRemoteAddressFromRequest(SIPRequest request, boolean sipUseSourceIpAsRemoteAddress) {

        String remoteAddress;
        int remotePort;
        if (sipUseSourceIpAsRemoteAddress) {
            remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
            remotePort = request.getPeerPacketSourcePort();

        } else {
            // 判断RPort是否改变，改变则说明路由nat信息变化，修改设备信息
            // 获取到通信地址等信息
            remoteAddress = request.getTopmostViaHeader().getReceived();
            remotePort = request.getTopmostViaHeader().getRPort();
            // 解析本地地址替代
            if (ObjectUtils.isEmpty(remoteAddress) || remotePort == -1) {
                remoteAddress =
                    Optional.ofNullable(request.getPeerPacketSourceAddress()).map(InetAddress::getHostAddress).orElse(request.getViaHost());
                remotePort = OptionalInt.of(request.getPeerPacketSourcePort()).stream().filter(e -> e != 0).findFirst().orElse(request.getViaPort());
            }
        }

        return new RemoteAddressInfo(remoteAddress, remotePort);
    }

    public static <T> T parseRequest(RequestEvent event, String charset, Class<T> clazz) {
        SIPRequest sipRequest = (SIPRequest)event.getRequest();
        return getObj(charset, clazz, sipRequest.getRawContent());
    }

    public static String parseRequest(RequestEvent event, String charset) {
        SIPRequest sipRequest = (SIPRequest)event.getRequest();
        byte[] rawContent = sipRequest.getRawContent();
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        return StringTools.toEncodedString(rawContent, Charset.forName(charset));
    }

    public static <T> T getObj(String charset, Class<T> clazz, byte[] rawContent) {
        if (StringUtils.isBlank(charset)) {
            charset = Constant.UTF_8;
        }
        String xmlStr = StringTools.toEncodedString(rawContent, Charset.forName(charset));
        Object o = XmlUtils.parseObj(xmlStr, clazz);
        return (T)o;
    }

    public static <T> T parseResponse(ResponseEvent evt, Class<T> tClass) {
        return parseResponse(evt, null, tClass);
    }

    public static <T> T parseResponse(ResponseEvent evt, String charset, Class<T> clazz) {
        Response response = evt.getResponse();
        return getObj(charset, clazz, response.getRawContent());
    }

    /**
     * 将 LocalDateTime 转换为 NTP 时间戳（SDP 时间格式）
     *
     * @param dateTime 本地时间
     * @return NTP 时间戳（秒）
     */
    public static long toNtpTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        // 转换为 UTC 时间戳（秒）
        long unixTimestamp = dateTime.toEpochSecond(ZoneOffset.UTC);
        // 加上 NTP 偏移量得到 NTP 时间戳
        return unixTimestamp + NTP_TIMESTAMP_OFFSET;
    }

    /**
     * 将时间字符串转换为 NTP 时间戳（SDP 时间格式）
     * 支持 ISO 8601 格式：2024-01-01T08:00:00
     *
     * @param timeString 时间字符串
     * @return NTP 时间戳（秒）
     */
    public static long toNtpTimestamp(String timeString) {
        if (StringUtils.isBlank(timeString)) {
            return 0;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeString);
            return toNtpTimestamp(dateTime);
        } catch (Exception e) {
            // 如果解析失败，返回0（SDP中表示永久会话）
            return 0;
        }
    }
}