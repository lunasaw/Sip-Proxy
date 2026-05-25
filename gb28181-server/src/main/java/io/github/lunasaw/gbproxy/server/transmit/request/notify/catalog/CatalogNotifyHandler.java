package io.github.lunasaw.gbproxy.server.transmit.request.notify.catalog;

import io.github.lunasaw.gbproxy.server.transmit.event.ServerQueryResponseEvent;

import javax.sip.RequestEvent;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.sip.common.transmit.event.message.MessageHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceOtherUpdateNotify;
import io.github.lunasaw.gbproxy.server.transmit.request.notify.NotifyServerHandlerAbstract;
import io.github.lunasaw.sip.common.entity.DeviceSession;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181-2022 §9.11.4 / §A.2.5 Catalog NOTIFY 处理器
 * (rootType=Notify, cmdType=Catalog, method=MESSAGE)。
 *
 * <p>v1.5.6 修正：此前 {@code getRootType()} 返回 "NOTIFYResponse" + 未 override {@code getMethod()}
 * 导致注册 key="NOTIFYResponse" + "null_Catalog"，与 dispatch 实际查找的 key 完全不匹配 —
 * 设备目录变更通知此前一直被静默丢弃。
 *
 * <p>修正后：
 * <ul>
 *   <li>rootType=Notify（来自 XML 根元素 {@code <Notify>}）</li>
 *   <li>method=MESSAGE（项目约定：{@code ClientCommandSender.sendCatalogChangeNotify} 使用 SIP MESSAGE 方法
 *       承载 Notify 体；GB28181-2022 §9.11.4 同时允许 SIP NOTIFY 方法，但本项目走 MESSAGE 通道）</li>
 *   <li>cmdType=Catalog</li>
 * </ul>
 *
 * <p>设备主动发送目录变更通知（设备上线/下线/添加/删除/属性更新）时，解析 {@link DeviceOtherUpdateNotify}
 * 并发布 {@link ServerQueryResponseEvent}，由 {@code ServerListenerAdapter} 路由到
 * {@code DeviceResponseListener.onNotifyUpdate}。
 *
 * @author luna
 */
@Component
@Slf4j
@Getter
@Setter
public class CatalogNotifyHandler extends NotifyServerHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";

    public CatalogNotifyHandler(ApplicationEventPublisher publisher, ServerDeviceSupplier serverDeviceSupplier) {
        super(publisher, serverDeviceSupplier);
    }

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceSession deviceSession = getDeviceSession(event);

        String userId = deviceSession.getUserId();
        Device device = serverDeviceSupplier.getDevice(userId);
        if (device == null) {
            // 未注册的设备不做处理
            return;
        }

        DeviceOtherUpdateNotify deviceOtherUpdateNotify = parseXml(DeviceOtherUpdateNotify.class);

        publisher.publishEvent(new ServerQueryResponseEvent(this, userId, null, deviceOtherUpdateNotify));
    }

    @Override
    public String getCmdType() {
        return CMD_TYPE;
    }

    @Override
    public String getRootType() {
        return MessageHandler.NOTIFY;
    }

    @Override
    public String getMethod() {
        return "MESSAGE";
    }
}
