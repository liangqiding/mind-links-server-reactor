package com.mind.links.netty.mqtt.kafka;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import entity.MqttPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author qiDing
 * date: 2021-01-03 14:32
 * @version v1.0.0
 * description
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducers {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic:}")
    private String topic;

    /**
     * topic 过滤 顺序很重要,此写法需先匹配长的
     */
    public String kafkaFilter(String topic) {
        boolean one = topic.startsWith("i2dsp/Emg/1");
        boolean two = topic.startsWith("i2dsp/Emg/2");
        boolean three = topic.startsWith("i2dsp/Emg/3");
        boolean four = topic.startsWith("i2dsp/Emg/4");
        boolean twentyTwo = topic.startsWith("i2dsp/Emg/22");
        return twentyTwo
                ? "MessageLv22" : one
                ? "MessageLv1" : two
                ? "MessageLv2" : three
                ? "MessageLv3" : four
                ? "MessageLv4" : "";
    }

    /**
     * 消息转本地保存
     */
    public Mono<Void> sendAndSave(MqttPacket mqttPacket) {
        return Mono.just(mqttPacket.getTopic())
                .map(this::kafkaFilter)
                .filter(s -> s != null && !"".equals(s))
                .doOnNext(s -> log.info("topic:" + s))
                .flatMap(t -> Mono.fromCallable(() ->
                        kafkaTemplate.send(t, this.toKafkaTem(mqttPacket))))
                .then();
    }

    public String toKafkaTem(MqttPacket mqttPacket) {
        JSONObject body = new JSONObject();
        body.put("topic", mqttPacket.getTopic());
        body.put("payload", new String(mqttPacket.getMessageBytes()));
        body.put("timestamp", mqttPacket.getTimestamp());
        return body.toJSONString();
    }

    /**
     * 集群下通过brokerId通知其它服务器进行发送 ifKafka 为false才进行发送,如果是消息是已经是kafka发送的，不继续发，防止死循环
     */
    public Mono<Void> sendPub(MqttPacket mqttPacket, boolean ifKafka) {
        return Mono.just(mqttPacket)
                .filter(m -> !ifKafka)
                .filter(Objects::nonNull)
                .flatMap(b -> Mono
                        .fromCallable(() -> kafkaTemplate.send(mqttPacket.getBrokerId(), JSON.toJSONString(mqttPacket))))
                .doOnNext(s -> log.info("==非本机连接,通过kafka-brokerId通知其它服务器进行发送==="))
                .then();
    }

}
