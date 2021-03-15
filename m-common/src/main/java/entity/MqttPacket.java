package entity;

import annotation.Desc;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * mqtt消息包
 *
 * @author qiding
 */
@Data
@Accessors(chain = true)
public class MqttPacket implements Serializable {

    private static final long serialVersionUID = -1L;

    @Desc("经纪人编号")
    private String brokerId;

    @Desc("封包编号")
    private Integer packetId;

    @Desc("接受客户端id")
    private String clientId;

    @Desc("主题")
    private String topic;

    @Desc("消息质量,0表示最多一次 即<=1,1表示至少一次 即>=1,2表示仅发送一次")
    private int qoS;

    @Desc("发布端消息质量")
    private MqttQoS reqQoS;

    @Desc("接受端消息质量")
    private MqttQoS respQoS;

    @Desc("消息体")
    private byte[] messageBytes;

    @Desc("是否保留")
    private boolean retain;

    @Desc("其是用来在保证消息传输可靠的，如果设置为true，则在下面的变长头部里多加MessageId,并需要回复确认，保证消息传输完成，但不能用于检测消息重复发送")
    private boolean dup;

    @Desc("时间戳")
    private Long timestamp;

    public MqttPacket(MqttPublishMessage msg, String clientId) {
        this.clientId = clientId;
        this.topic = msg.variableHeader().topicName();
        this.qoS = msg.fixedHeader().qosLevel().value();
        this.reqQoS = msg.fixedHeader().qosLevel();
        this.messageBytes = msgBytes(msg);
        this.retain = msg.fixedHeader().isRetain();
        this.dup = msg.fixedHeader().isDup();
    }

    public MqttPacket() {

    }

    public byte[] msgBytes(MqttPublishMessage msg) {
        byte[] messageBytes = new byte[msg.payload().readableBytes()];
        msg.payload().getBytes(msg.payload().readerIndex(), messageBytes);
        return messageBytes;
    }
}
