package entity;

import annotation.Desc;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * PUB_REL qos 第二步确认存储
 * @author qiding
 */
@Data
@Accessors(chain = true)
public class DupPubRelMessage implements Serializable {

	private static final long serialVersionUID = -4111642532532950980L;

	@Desc("客户端id")
	private String clientId;

	@Desc("消息id (messageId)")
	private int packetId;

	public DupPubRelMessage(String clientId, int packetId) {
		this.clientId = clientId;
		this.packetId = packetId;
	}

	public DupPubRelMessage(){

	}
}
