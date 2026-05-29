package io.github.lunasaw.gb28181.common.entity.control;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.*;

/**
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlIFame extends DeviceControlBase {


    @XmlElement(name = "IFameCmd")
    public String iFameCmd;

    public DeviceControlIFame(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("IFameCmd");
    }


}
