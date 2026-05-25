package io.github.lunasaw.sip.common.utils;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlUtilsTest {

    @XmlRootElement(name = "Message")
    static class TestDto {
        @XmlElement(name = "CmdType")
        public String cmdType;
        @XmlElement(name = "SN")
        public String sn;
    }

    private static final String QUERY_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Query>\n" +
            "  <CmdType>DeviceInfo</CmdType>\n" +
            "  <SN>1</SN>\n" +
            "</Query>";

    private static final String NO_CMDTYPE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Notify><Status>OK</Status></Notify>";

    @Test
    void getCmdType_returnsValue() {
        assertThat(XmlUtils.getCmdType(QUERY_XML)).isEqualTo("DeviceInfo");
    }

    @Test
    void getCmdType_missingElement_returnsNull() {
        assertThat(XmlUtils.getCmdType(NO_CMDTYPE_XML)).isNull();
    }

    @Test
    void getCmdType_withBom_parsesCorrectly() {
        String withBom = "﻿" + QUERY_XML;
        assertThat(XmlUtils.getCmdType(withBom)).isEqualTo("DeviceInfo");
    }

    @Test
    void getRootType_returnsRootElementName() {
        assertThat(XmlUtils.getRootType(QUERY_XML)).isEqualTo("Query");
        assertThat(XmlUtils.getRootType(NO_CMDTYPE_XML)).isEqualTo("Notify");
    }

    @Test
    void toString_and_parseObj_roundTrip() {
        TestDto dto = new TestDto();
        dto.cmdType = "Keepalive";
        dto.sn = "42";

        String xml = XmlUtils.toString("UTF-8", dto);
        assertThat(xml).contains("Keepalive").contains("42");

        TestDto parsed = (TestDto) XmlUtils.parseObj(xml, TestDto.class);
        assertThat(parsed.cmdType).isEqualTo("Keepalive");
        assertThat(parsed.sn).isEqualTo("42");
    }
}
