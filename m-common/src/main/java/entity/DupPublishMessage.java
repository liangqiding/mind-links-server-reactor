package entity;

import annotation.Desc;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * PUBLISH重发消息存储
 *
 * @author qiding
 */
@Data
@Accessors(chain = true)
public class DupPublishMessage implements Serializable {

	private static final long serialVersionUID = -8112511377194421600L;

	@Desc("客户端id")
	private String clientId;

	@Desc("topic")
	private String topic;

	@Desc("qos")
	private int mqttQoS;

	@Desc("消息id (messageId)")
	private int packetId;

	@Desc("消息体")
	private byte[] messageBytes;

	public DupPublishMessage(MqttPacket mqttPacket) {
		this.clientId = mqttPacket.getClientId();
		this.topic = mqttPacket.getTopic();
		this.mqttQoS = mqttPacket.getRespQoS().value();
		this.packetId = mqttPacket.getPacketId();
		this.messageBytes = mqttPacket.getMessageBytes();
	}

	public DupPublishMessage() {

	}
}
