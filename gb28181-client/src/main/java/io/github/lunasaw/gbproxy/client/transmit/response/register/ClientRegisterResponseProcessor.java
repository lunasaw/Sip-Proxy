package io.github.lunasaw.gbproxy.client.transmit.response.register;

import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.gbproxy.client.transmit.response.ClientAbstractSipResponseProcessor;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.service.TimeSyncService;
import io.github.lunasaw.sip.common.transmit.SipSender;
import io.github.lunasaw.sip.common.transmit.request.SipRequestBuilderFactory;
import io.github.lunasaw.sip.common.utils.SipUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.header.DateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.Calendar;

/**
 * Register响应处理器
 * 只负责SIP协议层面的处理，业务逻辑通过RegisterProcessorHandler接口实现
 * 这个是客户端发起的REGISTER后，服务端回复的REGISTER响应处理器
 *
 * @author luna
 */
@Slf4j
@Getter
@Setter
@Component("clientRegisterResponseProcessor")
public class ClientRegisterResponseProcessor extends ClientAbstractSipResponseProcessor {

    public static final String METHOD = "REGISTER";

    private String method = METHOD;

    @Autowired
    private ClientDeviceSupplier deviceSupplier;

    @Autowired
    @Lazy
    private RegisterProcessorHandler registerProcessorHandler;

    @Autowired
    private TimeSyncService timeSyncService;

    /**
     * 处理Register响应
     *
     * @param evt 事件
     */
    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            if (StringUtils.isBlank(callId)) {
                log.warn("Register响应处理失败：callId为空");
                return;
            }

            String toUserId = SipUtils.getUserIdFromToHeader(response);
            int statusCode = response.getStatusCode();

            if (statusCode == Response.UNAUTHORIZED) {
                handleUnauthorizedResponse(evt, toUserId, callId);
            } else if (statusCode == Response.OK) {
                // 处理注册成功，包括时间同步
                handleRegisterSuccess(response, toUserId);
            } else {
                registerProcessorHandler.handleRegisterFailure(toUserId, statusCode);
                log.warn("Register失败：toUserId = {}, statusCode = {}", toUserId, statusCode);
            }
        } catch (Exception e) {
            log.error("处理Register响应异常：evt = {}", evt, e);
        }
    }

    /**
     * 处理未授权响应
     *
     * @param evt      响应事件
     * @param toUserId 目标用户ID
     * @param callId   呼叫ID
     */
    private void handleUnauthorizedResponse(ResponseEvent evt, String toUserId, String callId) {
        try {
            ResponseEventExt eventExt = (ResponseEventExt) evt;
            SIPResponse response = (SIPResponse) evt.getResponse();

            // 调用业务处理器
            registerProcessorHandler.handleUnauthorized(evt, toUserId, callId);

            // 协议层面的重新认证处理
            processReAuthentication(eventExt, toUserId, callId);

        } catch (Exception e) {
            log.error("处理未授权响应异常：toUserId = {}, callId = {}", toUserId, callId, e);
        }
    }

    /**
     * 处理重新认证
     *
     * @param evt      响应事件
     * @param toUserId 目标用户ID
     * @param callId   呼叫ID
     */
    private void processReAuthentication(ResponseEventExt evt, String toUserId, String callId) {
        SIPResponse response = (SIPResponse) evt.getResponse();
        CallIdHeader callIdHeader = response.getCallIdHeader();

        FromDevice fromDevice = deviceSupplier.getClientFromDevice();
        ToDevice toDevice = deviceSupplier.getToDevice(deviceSupplier.getDevice(toUserId));

        if (fromDevice == null || toDevice == null) {
            log.error("设备信息获取失败：fromDevice = {}, toDevice = {}", fromDevice, toDevice);
            return;
        }

        WWWAuthenticateHeader www = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
        if (www == null) {
            log.error("未找到WWW-Authenticate头");
            return;
        }

        Integer expire = registerProcessorHandler.getExpire(toUserId);
        Request registerRequestWithAuth = SipRequestBuilderFactory.createRegisterRequestWithAuth(
                fromDevice, toDevice, callIdHeader.getCallId(), expire, www);

        // 发送二次请求
        SipSender.transmitRequest(fromDevice.getIp(), registerRequestWithAuth);
        log.info("发送重新认证请求：toUserId = {}, callId = {}", toUserId, callId);
    }

    /**
     * 处理注册成功响应，包括时间同步
     *
     * @param response 注册成功响应
     * @param toUserId 目标用户ID
     */
    private void handleRegisterSuccess(SIPResponse response, String toUserId) {
        try {
            // 调用业务处理器
            registerProcessorHandler.registerSuccess(toUserId);
            log.info("Register成功：toUserId = {}", toUserId);

            // 处理SIP校时 - 从Date头域同步时间
            handleSipTimeSync(response);

        } catch (Exception e) {
            log.error("处理注册成功响应异常：toUserId = {}", toUserId, e);
        }
    }

    /**
     * 处理SIP校时
     *
     * @param response SIP响应消息
     */
    private void handleSipTimeSync(SIPResponse response) {
        try {
            DateHeader dateHeader = (DateHeader) response.getHeader(DateHeader.NAME);
            if (dateHeader == null) {
                log.debug("未找到Date头域，跳过SIP校时");
                return;
            }

            // 获取Date头域的值
            Calendar calendar = dateHeader.getDate();
            // 将Calendar转换为标准的ISO格式字符串
            String dateValue = String.format("%04d-%02d-%02dT%02d:%02d:%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH 是从0开始的
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND));
            log.debug("收到Date头域：{}", dateValue);

            // 执行时间同步
            boolean syncSuccess = timeSyncService.syncTimeFromSip(dateValue);
            if (syncSuccess) {
                log.info("SIP校时成功");
                
                // 检查是否需要进一步校时
                if (timeSyncService.needsTimeSync()) {
                    log.warn("时间偏差仍然较大，建议检查系统时间设置");
                }
            } else {
                log.warn("SIP校时失败");
            }

        } catch (Exception e) {
            log.error("SIP校时处理异常", e);
        }
    }
}
