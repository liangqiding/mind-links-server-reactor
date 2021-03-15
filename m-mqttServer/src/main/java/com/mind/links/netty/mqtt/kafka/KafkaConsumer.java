package com.mind.links.netty.mqtt.kafka;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mind.links.netty.mqtt.mqttHandler.messageHandler.PublishHandler;
import entity.MqttPacket;
import entity.MqttSession;
import exception.LinksExceptionTcp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.function.Function;

/**
 * date: 2021-01-23 09:02
 * description 集群模式下消息转发
 *
 * @author qiDing
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class KafkaConsumer {

    private final PublishHandler publishHandler;

    private final Scheduler myScheduler;

    public static final Function<Object, MqttPacket> TO_MQTT_PACKET = o -> JSON.parseObject(o.toString(), MqttPacket.class);

    @Value("${links.mqtt.broker.id:}")
    private String brokerId;

    @KafkaListener(topics = "${links.mqtt.broker.id:}", groupId = "${links.mqtt.broker.id:}")
    public void listener(List<ConsumerRecord<?, ?>> consumerRecords) {
        log.info("======集群模式接收到发布通知=brokerId=={},size:{}", brokerId, consumerRecords.size());
        Flux.fromIterable(consumerRecords)
                .doOnNext(mq -> log.info("===集群模式接受到发布通知：{}", JSON.toJSONString(mq)))
                .map(ConsumerRecord::value)
                .flatMap(this::sendPub)
                .subscribeOn(myScheduler)
                .subscribe();
    }

    /**
     * 设置 ifKafka=true 告诉发送处理函数，这是一个kafka的消息
     */
    public Mono<Boolean> sendPub(Object o) {
        return Mono.just(o)
                .map(TO_MQTT_PACKET)
                .flatMap(mqttPacket -> publishHandler.sendPublishMessage(mqttPacket, true))
                .onErrorResume(LinksExceptionTcp::errors);
    }
}
