package entity;

import annotation.Desc;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * 订阅存储 订阅实体类
 * @author qiding
 */
@Data
@Accessors(chain = true)
public class MqttSubscribe implements Serializable {

	private static final long serialVersionUID = -1;

    @Desc("客户端id")
	private String clientId;

	@Desc("主题")
	private String topic;

	@Desc("消息等级")
	private Integer mqttQoS;

	public MqttSubscribe(String clientId, String topic, Integer mqttQoS) {
		this.clientId = clientId;
		this.topic = topic;
		this.mqttQoS = mqttQoS;
	}
	public MqttSubscribe(){

	}
}
