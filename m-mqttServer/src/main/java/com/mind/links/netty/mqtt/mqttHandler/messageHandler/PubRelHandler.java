package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 16:00
 * description 发布已释放（qos2 第二步）
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PubRelHandler implements IMqttMessageHandler {

    @Override
    public Mono<Channel> pubRel(Channel channel, MqttMessage msg) {
        return this.handler(channel, (MqttMessageIdVariableHeader) msg.variableHeader());
    }

    public Mono<Channel> handler(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        final String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        final int messageId = variableHeader.messageId();
        log.debug("发布已释放（qos2 第二步pubRel）- clientId: {}, messageId: {}", clientId, messageId);
        return this.okResponse(channel, variableHeader);
    }

    public Mono<Channel> okResponse(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        final MqttMessage pubCompMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(variableHeader.messageId()), null);
        return Mono.just(channel.writeAndFlush(pubCompMessage))
                .then(Mono.just(channel));
    }

}
