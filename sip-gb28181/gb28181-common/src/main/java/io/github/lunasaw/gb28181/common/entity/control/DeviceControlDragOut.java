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
 * <?xml version="1.0" encoding="UTF-8"?>
 * <Control>
 * <CmdType>DeviceControl</CmdType>
 * <SN>331004</SN>
 * <DeviceID>1231</DeviceID>
 * <DragZoomOut>
 * <Length>dragZoom.getLength()</Length>
 * <Width>dragZoom.getWidth()</Width>
 * <MidPointX>ragZoom.getMidPointX()</MidPointX>
 * <MidPointY> dragZoom.getMidPointY()</MidPointY>
 * <LengthX>ragZoom.getLengthX()</LengthX>
 * <LengthY> dragZoom.getLengthY()</LengthY>
 * </DragZoomOut>
 * </Control>
 *
 * @author luna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "Control")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeviceControlDragOut extends DeviceControlBase {


    /**
     * 缩小
     */
    @XmlElement(name = "DragZoomOut")
    private DragZoom dragZoomOut;

    public DeviceControlDragOut(String cmdType, String sn, String deviceId) {
        super(cmdType, sn, deviceId);
        this.setControlType("DragZoomOut");
    }

    public static void main(String[] args) {
        DeviceControlDragOut deviceControlDrag = new DeviceControlDragOut();

        deviceControlDrag.setDragZoomOut(new DragZoom("1", "2", "3", "4", "5", "6"));

        System.out.println(deviceControlDrag);
    }

}
