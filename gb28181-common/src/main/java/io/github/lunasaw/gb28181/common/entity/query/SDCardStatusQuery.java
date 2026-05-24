package io.github.lunasaw.gb28181.common.entity.query;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import io.github.lunasaw.gb28181.common.entity.base.DeviceBase;
import lombok.Getter;
import lombok.Setter;

/**
 * GB28181-2022 A.2.4.14 存储卡状态查询请求 (cmdType=SDCardStatus)
 *
 * @author luna
 */
@Getter
@Setter
@XmlRootElement(name = "Query")
@XmlAccessorType(XmlAccessType.FIELD)
public class SDCardStatusQuery extends DeviceBase {

    public SDCardStatusQuery() {
    }

    public SDCardStatusQuery(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
    }
}
