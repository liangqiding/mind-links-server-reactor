package entity;


import annotation.Desc;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Data;
import lombok.experimental.Accessors;
import utils.SessionUtil;
import java.io.Serializable;
import java.util.Objects;

/**
 * 会话实体类 加入存储频道和客户端的关系 便于分布式集群部署
 *
 * @author qidingliang
 */
@Data
@Accessors(chain = true)
public class MqttSession implements Serializable {

    private static final long serialVersionUID = -1L;

    @Desc("服务器id")
    private String brokerId;

    @Desc("客户端id")
    private String clientId;

    @Desc("管道id")
    private String channelId;

    @Desc("到期时间")
    private Integer expire;

    @Desc("清空session")
    private boolean cleanSession;

    @Desc("是否设置遗愿,默认false")
    private boolean isWillFlag;

    @Desc("Mqtt遗嘱消息")
    private MqttPublishMessage willMessage;

    public MqttSession() {
        this.isWillFlag = false;
    }

    public MqttSession(String brokerId, String clientId, String channelId, Integer expire, boolean cleanSession) {
        this.brokerId = brokerId;
        this.clientId = clientId;
        this.channelId = channelId;
        this.expire = expire;
        this.cleanSession = cleanSession;
        this.isWillFlag = false;
    }

    @Override
    public String toString() {
        return Objects.requireNonNull(SessionUtil.transPublishToJson(this)).toJSONString();
    }
}
