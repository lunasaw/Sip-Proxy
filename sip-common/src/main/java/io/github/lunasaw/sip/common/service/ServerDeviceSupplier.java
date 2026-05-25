package io.github.lunasaw.sip.common.service;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.utils.SipUtils;

import javax.sip.RequestEvent;

/**
 * 服务端设备提供器接口
 * 扩展DeviceSupplier接口，提供服务端特定的设备获取能力
 * <p>
 * 设计原则：
 * 1. 继承基础设备提供器接口，保持接口的一致性
 * 2. 提供服务端发送方设备信息获取能力
 * 3. 支持服务端SIP消息发送时的设备标识
 *
 * @author luna
 * @date 2025/01/23
 */
public interface ServerDeviceSupplier extends DeviceSupplier {

    /**
     * 获取服务端发送方设备信息
     * 用于服务端发送SIP消息时标识发送方设备
     *
     * @return 服务端发送方设备信息，如果不存在则返回null
     */
    FromDevice getServerFromDevice();

    /**
     * 设置服务端发送方设备信息
     * 用于配置服务端的发送方设备标识
     *
     * @param fromDevice 服务端发送方设备信息
     */
    void setServerFromDevice(FromDevice fromDevice);

    /**
     * 设备检查
     *
     * @param evt
     * @return
     */
    default boolean checkDevice(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();

        // 在接收端看来 收到请求的时候fromHeader还是服务端的 toHeader才是自己的，这里是要查询自己的信息
        String userId = SipUtils.getUserIdFromToHeader(request);

        // 获取当前作为服务端的配置
        FromDevice fromDevice = getServerFromDevice();

        if (fromDevice == null) {
            return false;
        }
        return userId.equals(fromDevice.getUserId());
    }

    /**
     * 设备注册鉴权（HTTP Digest）。
     *
     * <p>业务方典型实现：取出 userId 对应明文密码，调
     * {@code DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, password)} 比对。
     *
     * <p>默认放行，便于不需要鉴权的内网/测试环境快速接入。
     * <strong>生产环境必须显式覆盖此方法</strong>，否则等于关闭鉴权。
     *
     * @param userId  设备 ID（来自 From-Header）
     * @param request 完整 SIP 请求，含 AuthorizationHeader
     * @return true 通过，false 拒绝注册（处理器返回 403）
     */
    default boolean authenticate(String userId, SIPRequest request) {
        return true;
    }
}