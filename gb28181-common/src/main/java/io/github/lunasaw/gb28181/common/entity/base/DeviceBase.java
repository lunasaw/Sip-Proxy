package io.github.lunasaw.gb28181.common.entity.base;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import lombok.Getter;
import lombok.Setter;

/**
 * @author luna
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceBase extends XmlBean {
    @XmlElement(name = "CmdType")
    private String cmdType;

    @XmlElement(name = "SN")
    private String sn;

    @XmlElement(name = "DeviceID")
    private String deviceId;

    public DeviceBase() {
    }

    public DeviceBase(String cmdType, String sn, String deviceId) {
        this.cmdType = cmdType;
        this.sn = sn;
        this.deviceId = deviceId;
    }


}
