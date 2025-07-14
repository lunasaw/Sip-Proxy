package io.github.lunasaw.gb28181.common.entity.control;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author luna
 */
@Getter
@Setter
public class DeviceControlBase extends DeviceBase {

    @XmlElement(name = "ControlType")
    private String controlType;

    public DeviceControlBase() {
    }

    public DeviceControlBase(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
