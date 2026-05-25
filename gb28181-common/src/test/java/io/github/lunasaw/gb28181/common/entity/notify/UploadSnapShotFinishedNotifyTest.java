package io.github.lunasaw.gb28181.common.entity.notify;

import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.xml.XmlBean;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB28181-2022 A.2.5.7 图像抓拍传输完成通知 JAXB 往返测试。
 */
class UploadSnapShotFinishedNotifyTest {

    @Test
    void marshalAndUnmarshal_shouldPreserveAllFields() {
        UploadSnapShotFinishedNotify notify = new UploadSnapShotFinishedNotify("12345", "34020000001320000001");
        notify.setSessionId("abcdef0123456789abcdef0123456789ab");
        notify.setSnapShotFileIds(Arrays.asList("snap-001", "snap-002", "snap-003"));

        String xml = notify.toString();
        assertThat(xml).contains("<CmdType>" + CmdTypeEnum.UPLOAD_SNAP_SHOT_FINISHED.getType() + "</CmdType>");
        assertThat(xml).contains("<SessionID>abcdef0123456789abcdef0123456789ab</SessionID>");
        assertThat(xml).contains("<SnapShotList>");
        assertThat(xml).contains("<SnapShotFileID>snap-001</SnapShotFileID>");
        assertThat(xml).contains("<SnapShotFileID>snap-002</SnapShotFileID>");
        assertThat(xml).contains("<SnapShotFileID>snap-003</SnapShotFileID>");

        UploadSnapShotFinishedNotify parsed = (UploadSnapShotFinishedNotify) XmlBean.parseObj(xml, UploadSnapShotFinishedNotify.class);
        assertThat(parsed.getCmdType()).isEqualTo("UploadSnapShotFinished");
        assertThat(parsed.getSn()).isEqualTo("12345");
        assertThat(parsed.getDeviceId()).isEqualTo("34020000001320000001");
        assertThat(parsed.getSessionId()).isEqualTo("abcdef0123456789abcdef0123456789ab");

        List<String> ids = parsed.getSnapShotFileIds();
        assertThat(ids).containsExactly("snap-001", "snap-002", "snap-003");
    }
}
