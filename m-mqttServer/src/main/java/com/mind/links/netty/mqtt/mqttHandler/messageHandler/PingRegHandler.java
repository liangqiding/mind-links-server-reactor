package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import entity.MqttSession;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/**
 * date: 2021-01-13 10:12
 * description PING_REQ ping请求事件处理,
 *
 * @author qiDing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PingRegHandler implements IMqttMessageHandler {

    private final SessionServiceImpl sessionServiceImpl;

    private final BrokerProperties brokerProperties;


    @Override
    public Mono<Channel> pingReq(Channel channel) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        AtomicReference<MqttSession> mqttSession = new AtomicReference<>();
        return Mono.just(channel)
                .filterWhen(c -> sessionServiceImpl.containsKey(clientId))
                .flatMap(c -> sessionServiceImpl.getMqttSession(clientId))
                .map(m -> {
                    mqttSession.set(m);
                    return m;
                })
                .filterWhen(m ->  ChannelCommon.containsChannel(m.getChannelId()))
                .flatMap(m -> sessionServiceImpl.expire(clientId, mqttSession.get().getExpire()))
                .then(okResponse(channel, clientId));
    }

    public Mono<Channel> okResponse(Channel channel, String clientId) {
        return Mono.just(channel)
                .map(c -> {
                    MqttMessage pingRespMessage = MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0), null, null);
                    log.debug("PING_REQ - clientId: {}", clientId);
                    channel.writeAndFlush(pingRespMessage);
                    return c;
                });
    }

}
