package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GB28181协议 语音对讲查询
 * <p>
 * 语音对讲功能实现中心用户与前端用户之间的一对一语音对讲功能。
 * 语音对讲功能由两个独立的流程组合实现：
 * a）通过9.2的实时视音频点播功能，中心用户获得前端设备的实时视音频媒体流；
 * b）通过9.12的语音广播功能，中心用户向前端对讲设备发送实时音频媒体流。
 * </p>
 * <pre>
 * <Query>
 *   <CmdType>Talk</CmdType>
 *   <SN>123</SN>
 *   <DeviceID>34020000001360000001</DeviceID>
 *   <TargetID>34020000001370000001</TargetID>
 * </Query>
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class TalkQuery {
    
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
     * 语音输入设备的设备编码（必选）
     */
    @XmlElement(name = "DeviceID")
    private String deviceId;

    /**
     * 语音输出设备的设备编码（必选）
     */
    @XmlElement(name = "TargetID")
    private String targetId;
}