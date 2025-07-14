package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import lombok.Setter;

/**
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceMobileQuery extends XmlBean {
    @XmlElement(name = "CmdType")
    public String cmdType;

    @XmlElement(name = "SN")
    public String sn;

    @XmlElement(name = "DeviceID")
    public String deviceId;

    @XmlElement(name = "interval")
    private String Interval;

    public DeviceMobileQuery() {
    }

    public DeviceMobileQuery(String cmdType, String sn, String deviceId) {
        this.cmdType = cmdType;
        this.sn = sn;
        this.deviceId = deviceId;
    }


}
