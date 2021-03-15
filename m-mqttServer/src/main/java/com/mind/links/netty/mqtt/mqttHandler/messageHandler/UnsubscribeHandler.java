package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * date: 2021-01-14 16:07
 * description 取消订阅
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UnsubscribeHandler implements IMqttMessageHandler {

    private final SubscribeServiceImpl subscribeService;

    @Override
    public Mono<Channel> unsubscribe(Channel channel, MqttMessage msg) {
        log.debug("取消订阅:" + channel);
        return this.processUnSubscribe(channel, (MqttUnsubscribeMessage) msg);
    }

    public Mono<Channel> processUnSubscribe(Channel channel, MqttUnsubscribeMessage msg) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        List<String> topics = msg.payload().topics();
        log.debug("UNSUBSCRIBE - clientId: {}, topics: {}", clientId, topics);
        return Flux.fromIterable(topics)
                .flatMap(topic -> subscribeService.removeOne(clientId, topic))
                .collectList()
                .map(b -> (MqttUnsubAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()), null))
                .map(channel::writeAndFlush)
                .then(Mono.just(channel));
    }
}
