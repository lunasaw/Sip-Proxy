package io.github.lunasaw.gb28181.common.entity.xml;

import java.io.StringReader;
import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import lombok.*;

/**
 * @author luna
 * @date 2023/10/12
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.NONE)
@AllArgsConstructor
@NoArgsConstructor
public class XmlBean {

    /**
     * 字符集, 支持 UTF-8 与 GB2312
     */
    private String charset = "UTF-8";

    @SneakyThrows
    @Override
    public String toString() {
        JAXBContext jaxbContext = JAXBContext.newInstance(this.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, charset);

        StringWriter writer = new StringWriter();
        marshaller.marshal(this, writer);
        return writer.toString();
    }

    @SneakyThrows
    public static  <T> Object parseObj(String xmlStr, Class<T> clazz) {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringReader(xmlStr));
    }
}
