package io.github.lunasaw.gb28181.common.entity.control;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class DragZoom {

    @XmlElement(name = "Length")
    public String length;

    @XmlElement(name = "Width")
    public String width;
    @XmlElement(name = "MidPointX")
    public String midPointX;
    @XmlElement(name = "MidPointY")
    public String midPointY;
    @XmlElement(name = "LengthX")
    public String lengthX;
    @XmlElement(name = "LengthY")
    public String lengthY;
}