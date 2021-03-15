package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 15:52
 * description 发布确认
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PubAckHandler implements IMqttMessageHandler {

    private final DupMessageServiceImpl dupMessageService;

    @Override
    public Mono<Channel> pubAck(Channel channel, MqttMessage msg) {
        log.debug("发布确认" + channel);
        return handler(channel, (MqttMessageIdVariableHeader)msg.variableHeader())
                .then(Mono.just(channel));
    }

    public Mono<Boolean> handler(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        int messageId = variableHeader.messageId();
        log.debug("===发布确认 - clientId: {}, messageId: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get(), messageId);
        return dupMessageService
                .removeMapCache((String) channel.attr(AttributeKey.valueOf("clientId")).get(), messageId);
    }
}
