package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 语音对讲应答
 * <p>
 * 语音对讲功能的应答响应，表示对讲请求的处理结果。
 * </p>
 * <pre>
 * <Response>
 *   <CmdType>Talk</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001360000001</DeviceID>
 *   <Result>OK</Result>
 * </Response>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Response")
@XmlAccessorType(XmlAccessType.FIELD)
public class TalkResponse {
    
    /**
     * 命令类型：语音对讲（必选）
     */
    @XmlElement(name = "CmdType")
    private final String cmdType = "Talk";

    /**
     * 命令序列号（必选）
     */
    @XmlElement(name = "SN")
    private String sn;

    /**
     * 语音输出设备的设备编码（必选）
     */
    @XmlElement(name = "DeviceID")
    private String deviceId;

    /**
     * 执行结果标志（必选）
     * OK - 对讲建立成功
     * ERROR - 对讲建立失败
     */
    @XmlElement(name = "Result")
    private String result;
}