package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 16:02
 * description 发布完成（qos2 第三步)
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PubCompHandler implements IMqttMessageHandler {

    private final DupRelServiceImpl dupRelService;

    private final DupMessageServiceImpl dupMessageService;

    @Override
    public Mono<Channel> pubComp(Channel channel, MqttMessage msg) {
        log.debug("发布完成（qos2 第三步 释放dupRel和msg)" + channel);
        return this.processPubComp(channel, (MqttMessageIdVariableHeader) msg.variableHeader());
    }

    /**
     * 释放缓存
     */
    public Mono<Channel> processPubComp(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        int messageId = variableHeader.messageId();
        return Mono.just(channel)
                .flatMap(c -> dupMessageService.removeMapCache(clientId, messageId))
                .flatMap(b -> dupRelService.removeMapKey(clientId, messageId))
                .then(Mono.just(channel))
                .doOnNext(c -> log.debug("发布完成（qos2 第三步)-释放dupRel- clientId: {}, messageId: {}", clientId, messageId));
    }
}
