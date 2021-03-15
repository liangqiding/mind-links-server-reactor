package entity;

import annotation.Desc;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Retain标志消息存储
 *
 * @author qiding
 */
@Data
@Accessors(chain = true)
public class RetainMessage implements Serializable {

    private static final long serialVersionUID = -7548204047370972779L;

    @Desc("topic")
    private String topic;

    @Desc("消息qos")
    private Integer mqttQoS;

    @Desc("消息内容")
    private byte[] messageBytes;

    public RetainMessage(String topic, Integer mqttQoS, byte[] messageBytes) {
        this.topic = topic;
        this.messageBytes = messageBytes;
        this.mqttQoS = mqttQoS;
    }

    public RetainMessage() {

    }
}
