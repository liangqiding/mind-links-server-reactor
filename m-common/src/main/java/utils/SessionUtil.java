package utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import entity.MqttPacket;
import entity.MqttSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Function;

/**
 * MqttSession对象不能正常转为JSON,我们定义一个工具类类进行特殊序列化
 * <p>
 *  主要目的是把 payload 的 ByteBuf 转换为 byte[]
 *
 * Created by  2019
 *
 * @author qiding
 */
@Slf4j
public class SessionUtil {

    /**
     * MqttSession转换成json
     */
    public static final Function<MqttSession, JSONObject> TO_JSON = mqttSession -> JSON.parseObject(JSON.toJSONString(mqttSession));

    /**
     * json转换成MqttSession
     */
    private static final Function<JSONObject, MqttSession> TO_MQTT_SESSION = mqttSession -> JSON.parseObject(mqttSession.toJSONString(), MqttSession.class);

    /**
     * 判断是否需要特殊序列化
     */
    private static final String FLAG = "willFlag";

    /**
     * 序列化 主要目的是把 payload  ByteBuf 转换为 byte[]
     */
    public static JSONObject transPublishToJson(MqttSession ms) {
        try {
            JSONObject mqttSession = TO_JSON.apply(ms);
            MqttPublishMessage mpm = ms.getWillMessage();
            if (ms.isWillFlag()) {
                JSONObject mqttPublishMessage = new JSONObject();
                mqttPublishMessage.put("payload", toBytes(mpm.payload()));
                mqttPublishMessage.put("messageType", mpm.fixedHeader().messageType().value());
                mqttPublishMessage.put("isDup", mpm.fixedHeader().isDup());
                mqttPublishMessage.put("qosLevel", mpm.fixedHeader().qosLevel().value());
                mqttPublishMessage.put("isRetain", mpm.fixedHeader().isRetain());
                mqttPublishMessage.put("remainingLength", mpm.fixedHeader().remainingLength());
                mqttPublishMessage.put("topicName", mpm.variableHeader().topicName());
                mqttPublishMessage.put("packetId", mpm.variableHeader().packetId());
                mqttPublishMessage.put("hasWillMessage", true);
                mqttSession.put("mqttPublishMessage", mqttPublishMessage);
            }
            return mqttSession;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 反序列化 转换 byte[] 为 ByteBuf
     */
    public static MqttSession transToMqttSession(JSONObject ms) {
        MqttSession mqttSession = TO_MQTT_SESSION.apply(ms);
        if (ms.getBoolean(FLAG)) {
            JSONObject mpm = ms.getJSONObject("mqttPublishMessage");
            ByteBuf byteBuf = toByteBuf(mpm.getBytes("payload"));
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                    MqttMessageType.valueOf(mpm.getInteger("messageType")),
                    mpm.getBoolean("isDup"),
                    MqttQoS.valueOf(mpm.getInteger("qosLevel")),
                    mpm.getBoolean("isRetain"),
                    mpm.getInteger("remainingLength")
            );
            MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(mpm.getString("topicName"),
                    mpm.getInteger("packetId"));
            MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader, mqttPublishVariableHeader, byteBuf);
            mqttSession.setWillMessage(mqttPublishMessage);
        }
        return mqttSession;
    }

    /**
     * ByteBuf -> byte[]
     */
    public static byte[] toBytes(ByteBuf byteBuf) {
        byte[] messageBytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), messageBytes);
        return messageBytes;
    }

    /**
     * byte[] -> ByteBuf
     */
    public static ByteBuf toByteBuf(byte[] payloads) {
        ByteBuf buf = null;
        try {
            buf = Unpooled.wrappedBuffer(payloads);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buf != null) {
                //   buf.release();
            }
        }
        return buf;
    }


}
