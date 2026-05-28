package io.github.lunasaw.gb28181.common.entity.notify;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Notify>
 * <CmdType>MediaStatus</CmdType>
 * <SN>226063</SN>
 * <DeviceID>12312</DeviceID>
 * <NotifyType>121</NotifyType>
 * </Notify>

 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Notify")
@XmlAccessorType(XmlAccessType.FIELD)
public class MediaStatusNotify extends DeviceBase {


    @XmlElement(name = "NotifyType")
    private String notifyType;

    public MediaStatusNotify(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }

}
