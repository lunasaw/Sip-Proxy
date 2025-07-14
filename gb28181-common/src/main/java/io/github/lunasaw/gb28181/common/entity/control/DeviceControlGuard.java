package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlGuard extends DeviceControlBase {

    @XmlElement(name = "GuardCmd")
    public String guardCmd;


    public DeviceControlGuard(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("GuardCmd");
    }


}
