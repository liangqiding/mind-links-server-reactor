package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import entity.DupPubRelMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 15:55
 * description 发布已接受（qos2 第一步）
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PubRecHandler implements IMqttMessageHandler {

    private final DupRelServiceImpl dupRelService;

    @Override
    public Mono<Channel> pubRec(Channel channel, MqttMessage msg) {
        log.debug("发布已接受（qos2 第一步）" + ChannelCommon.getClientId(channel));
        return this.processPubRec(channel, (MqttMessageIdVariableHeader) msg.variableHeader());
    }

    public Mono<Channel> processPubRec(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        int messageId = variableHeader.messageId();
        return dupRelService.putMapCache(new DupPubRelMessage(clientId, messageId))
                .flatMap(aBoolean -> okResponse(channel, variableHeader));
    }

    public Mono<Channel> okResponse(Channel channel, MqttMessageIdVariableHeader variableHeader) {
        MqttMessage pubRelMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(variableHeader.messageId()), null);
        return Mono.just(channel.writeAndFlush(pubRelMessage))
                .then(Mono.just(channel));
    }
}
