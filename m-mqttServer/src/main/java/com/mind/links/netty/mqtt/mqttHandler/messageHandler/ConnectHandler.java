package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.mqttFilter.MqttFilter;
import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import entity.MqttSession;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.bm.Lang;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import utils.SessionUtil;

/**
 * CONNECT连接报文处理
 *
 * @author qiding
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectHandler implements IMqttMessageHandler {

    private final SessionServiceImpl sessionServiceImpl;

    private final BrokerProperties brokerProperties;

    private final DupRelServiceImpl dupRelService;

    private final DupMessageServiceImpl dupMessageService;

    private final MqttFilter mqttFilter;


    @Override
    public Mono<Channel> connect(Channel channel, final MqttMessage mqttMessage) {
        MqttConnectMessage msg = (MqttConnectMessage) mqttMessage;
        return mqttFilter.filter(channel, msg)
                .flatMap(c -> this.handlerSession(c, msg)
                        .then(Mono.just(c)))
                .flatMap(c -> this.okResponse(channel, msg))
                .flatMap(c -> this.sendUndoneMessage(c, msg));
    }

    /**
     * 保存session
     */
    public Mono<Boolean> handlerSession(final Channel channel, MqttConnectMessage msg) {
        return this.refreshIdle(channel, msg)
                .flatMap(expire -> this.setMqttSession(channel, msg, expire))
                .flatMap(mqttSession -> sessionServiceImpl.saveMqttSession(mqttSession, mqttSession.getExpire()));
    }

    /**
     * 封装session 并设置遗嘱
     */
    public Mono<MqttSession> setMqttSession(Channel channel, MqttConnectMessage msg, int expire) {
        MqttSession mqttSession = new MqttSession(brokerProperties.getId(), msg.payload().clientIdentifier(), channel.id().asLongText(), expire, msg.variableHeader().isCleanSession());

        return Mono.just(mqttSession)
                .filter(m -> msg.variableHeader().isWillFlag())
                .map(m -> m.setWillFlag(true).setWillMessage((MqttPublishMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(msg.variableHeader().willQos()), msg.variableHeader().isWillRetain(), 0), new MqttPublishVariableHeader(msg.payload().willTopic(), 0), Unpooled.buffer().writeBytes(msg.payload().willMessageInBytes()))))
                .defaultIfEmpty(mqttSession);
    }

    /**
     * 刷新session有效时间
     */
    public Mono<Integer> refreshIdle(Channel channel, MqttConnectMessage msg) {
        return Mono.just(msg)
                .filter(message -> message.variableHeader().keepAliveTimeSeconds() > 0)
                .map(message -> {
                    int expire = 0;
                    if (channel.pipeline().names().contains("idle")) {
                        channel.pipeline().remove("idle");
                    }
                    expire = (Math.round(msg.variableHeader().keepAliveTimeSeconds() * 1.5f));
                    channel.pipeline().addFirst("idle", new IdleStateHandler(0, 0, expire));
                    return expire;
                })
                .doOnNext(expire -> log.debug("到期时间expire:" + expire));
    }

    public Mono<Channel> okResponse(final Channel channel, MqttConnectMessage msg) {
        return Mono.just(channel)
                .flatMap(c -> sessionServiceImpl.containsKey(msg.payload().clientIdentifier()))
                .map(sessionPresent -> (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent), null))
                .map(mqttConnAckMessage -> {
                    channel.attr(AttributeKey.valueOf("clientId")).set(msg.payload().clientIdentifier());
                    channel.writeAndFlush(mqttConnAckMessage);
                    log.debug("===3 CONNECT - clientId: {}, cleanSession: {}", msg.payload().clientIdentifier(), msg.variableHeader().isCleanSession());
                    return channel;
                });
    }

    /**
     * 如果cleanSession为0, 客户端CleanSession=0时，上线接收离线消息，源码分析,需要重发同一clientId存储的未完成的QoS1和QoS2的DUP消息
     */
    public Mono<Channel> sendUndoneMessage(final Channel channel, MqttConnectMessage msg) {
        log.debug("isCleanSession:" + msg.variableHeader().isCleanSession());
        final String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        return Mono.just(clientId)
                .filter(s -> !msg.variableHeader().isCleanSession())
                .flatMap(c -> sendDup(channel, c))
                .flatMap(c -> sendDupRel(channel, c))
                .then(Mono.just(channel));

    }

    /**
     * dup消息
     */

    public Mono<String> sendDup(final Channel channel, String clientId) {
        return dupMessageService.listMapCacheByClientId(clientId)
                .map(dupPublishMessage ->
                        (MqttPublishMessage) MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.valueOf(dupPublishMessage.getMqttQoS()), false, 0),
                                new MqttPublishVariableHeader(dupPublishMessage.getTopic(), dupPublishMessage.getPacketId()), Unpooled.buffer().writeBytes(dupPublishMessage.getMessageBytes())))
                .map(channel::writeAndFlush)
                .then(Mono.just(clientId));
    }

    /**
     * dup rel消息
     */
    public Mono<String> sendDupRel(final Channel channel, String clientId) {
        return dupRelService.listMapByClientId(clientId)
                .map(dupPubRelMessage -> MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBREL, true, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttMessageIdVariableHeader.from(dupPubRelMessage.getPacketId()), null))
                .map(channel::writeAndFlush)
                .then(Mono.just(clientId));
    }
}
