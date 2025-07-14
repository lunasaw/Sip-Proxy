package io.github.lunasaw.gb28181.common.entity.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

/**
 * @author luna
 * @date 2023/10/15
 */
@Getter
@Setter
@XmlRootElement(name = "Item")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceCatalog {

    /**
     * 设备/区域/系(必选)
     * 通道ID
     */
    @XmlElement(name = "DeviceID")
    private String deviceId;
    /**
     * 设备/区域/系(必选)
     */
    @XmlElement(name = "Name")
    private String name;
    /**
     * 当为设备时,设备厂商(必选)
     */
    @XmlElement(name = "Manufacturer")
    private String manufacturer;
    /**
     * 当为设备时,设备型号(必选)
     */
    @XmlElement(name = "Model")
    private String model;
    /**
     * 当为设备时,设备归属(必选)
     */
    @XmlElement(name = "Owner")
    private String owner;
    /**
     * 行政区域(必选)
     */
    @XmlElement(name = "CivilCode")
    private String civilCode;

    /**
     * 当为设备时,安装地址(必选)
     */
    @XmlElement(name = "Address")
    private String address;
    /**
     * 当为设备时,是否有子设备 (必选)1有,0没有
     */
    @XmlElement(name = "Parental")
    private int    parental    = 0;
    /**
     * 父设备/区域/系统ID(必选)
     */
    @XmlElement(name = "ParentID")
    private String parentId;

    /**
     * 信令安全模式(可选)缺省为0:不采用;2:S/MIME 签名方式;3:S/ MIME加密签名同时采用方式;4:数字摘要方式
     */
    @XmlElement(name = "SafetyWay")
    private int    safetyWay   = 0;
    /**
     * 注册方式(必选)缺省为1;1:符合IETFRFC3261标准的认证注册模 式;2:基于口令的双向认证注册模式;3:基于数字证书的双向认证注册模式
     */
    @XmlElement(name = "RegisterWay")
    private int    registerWay = 1;
    /**
     * 设备状态(必选)
     */
    @XmlElement(name = "Status")
    private String status;

    /**
     * 保密属性(必选)缺省为0 :不涉密,1:涉密
     */
    @XmlElement(name = "Secrecy")
    private int    secrecy     = 0;
}
