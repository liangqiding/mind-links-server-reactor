package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import com.mind.links.netty.mqtt.kafka.KafkaProducers;
import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.cache.MsgCache;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import entity.*;
import com.mind.links.netty.mqtt.mqttStore.cache.RetainMessageCache;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-13 15:38
 * description
 *
 * @author qiDing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishHandler implements IMqttMessageHandler {

    private final SessionServiceImpl sessionServiceImpl;

    private final SubscribeServiceImpl subscribeService;

    private final RetainMessageCache retainMessageCache;

    private final DupMessageServiceImpl dupMessageService;

    private final MsgCache msgCache;

    private final KafkaProducers kafkaProducers;

    private final BrokerProperties brokerProperties;

    @Override
    public Mono<Channel> publish(final Channel channel, final MqttMessage mqttMessage) {
        return this.processPublish(channel, (MqttPublishMessage) mqttMessage)
                .then(Mono.just(channel));
    }

    /**
     * 消息发布
     */
    public Mono<Void> processPublish(Channel channel, MqttPublishMessage msg) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        MqttPacket mqttPacket = new MqttPacket(msg, clientId).setTimestamp(System.currentTimeMillis());
        kafkaProducers.sendAndSave(mqttPacket)
                .then(Mono.just("消息转发至本地mq"))
                .subscribe(s -> log.info("===kafka==={}=topic:{},时间：{}", s, mqttPacket.getTopic(), mqttPacket.getTimestamp()));
        return sessionServiceImpl.extendSession(channel)
                .then(Mono.just(channel))
                .doOnNext(aBoolean -> log.info("===消息发布===topic:{},msg:{}", mqttPacket.getTopic(), new String(mqttPacket.getMessageBytes()).replaceAll("\n", "")))
                .then(this.isRetain(mqttPacket))
                .then(this.sendPublishMessage(mqttPacket))
                .flatMap(o -> {
                            // 应答确认-->回复客户端我们收到消息了 QoS=0 不回复
                            log.debug("===qos{}", msg.fixedHeader().qosLevel());
                            if (msg.fixedHeader().qosLevel() == MqttQoS.AT_MOST_ONCE) {
                                return Mono.empty();
                            }
                            // QoS=1 回复pubAck
                            if (msg.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                                return this.sendPubAckMessage(channel, msg.variableHeader().packetId());
                            }
                            // QoS=2 回复pubRec
                            if (msg.fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE) {
                                return this.sendPubRecMessage(channel, msg.variableHeader().packetId());
                            }
                            return Mono.empty();
                        }
                );
    }

    /**
     * 是否消息保留
     */
    public Mono<Boolean> isRetain(MqttPacket mqttPacket) {
        return Mono.just(mqttPacket)
                .filter(MqttPacket::isRetain)
                .flatMap(m -> retainMessageCache.putMapCache(new RetainMessage(m.getTopic(), m.getQoS(), m.getMessageBytes())))
                .defaultIfEmpty(true);
    }

    /**
     * 消息转发
     */
    public Mono<Boolean> sendPublishMessage(MqttPacket mqttPacket) {
        return this.sendPublishMessage(mqttPacket, false);
    }

    public Mono<Boolean> sendPublishMessage(MqttPacket mqttPacket, boolean ifKafka) {
        log.debug("===sendPublishMessage,topic:{},payload{}", mqttPacket.getTopic(), new String(mqttPacket.getMessageBytes()));
        return subscribeService.matchTopic(mqttPacket.getTopic())
                .filterWhen(mqttSubscribe -> sessionServiceImpl.containsKey(mqttSubscribe.getClientId()))
                .flatMap(mqttSubscribe -> {
                    log.debug("sendPublishMessage2:{}", mqttSubscribe);
                    MqttQoS respQoS = getMqttQoS(mqttSubscribe, mqttPacket.getReqQoS());
                    mqttPacket.setRespQoS(respQoS).setClientId(mqttSubscribe.getClientId());
                    return sessionServiceImpl
                            .getMqttSession(mqttSubscribe.getClientId())
                            // 是否包含频道
                            .filterWhen(mqttSession0 -> this.sendPubToKafka(mqttPacket.setBrokerId(mqttSession0.getBrokerId()), ifKafka, mqttSession0))
                            .flatMap(mqttSession -> {
                                // 接收质量确认
                                if (respQoS == MqttQoS.AT_MOST_ONCE) {
                                    return this.sendQos0(mqttSession, mqttSubscribe, mqttPacket.setPacketId(0));
                                }
                                if (respQoS == MqttQoS.AT_LEAST_ONCE) {
                                    return msgCache.getId(mqttPacket.getClientId())
                                            .flatMap(packetId -> this.sendQos1(mqttSession, mqttSubscribe, mqttPacket.setPacketId(packetId)));
                                }
                                if (respQoS == MqttQoS.EXACTLY_ONCE) {
                                    return msgCache.getId(mqttPacket.getClientId())
                                            .flatMap(packetId -> this.sendQos2(mqttSession, mqttSubscribe, mqttPacket.setPacketId(packetId)));
                                }
                                return Mono.empty();
                            });
                })
                .then(Mono.just(true));
    }

    /**
     * 若不是本机连接，则转发到其他broker处理
     */
    public Mono<Boolean> sendPubToKafka(MqttPacket mqttPacket, boolean ifKafka, MqttSession mqttSession) {
        return ChannelCommon.containsChannel(mqttSession.getChannelId())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(kafkaProducers.sendPub(mqttPacket, ifKafka).then(Mono.just(false)));
    }

    /**
     * qos 客户端的订阅等级， 最终取决于主题订阅的QoS，若发布的qos达不到订阅等级时，以发布的qos为准
     */
    private MqttQoS getMqttQoS(MqttSubscribe mqttSubscribe, MqttQoS mqttQoS) {
        return mqttQoS.value() > mqttSubscribe.getMqttQoS() ? MqttQoS.valueOf(mqttSubscribe.getMqttQoS()) : mqttQoS;
    }

    public Mono<Void> sendQos0(MqttSession mqttSession, MqttSubscribe mqttSubscribe, MqttPacket mqttPacket) {
        MqttPublishMessage publishMessage = responseFactory(mqttPacket);
        log.debug("sendQos0-发布 - clientId: {}, mqttMessage: {}", mqttSubscribe.getClientId(), JSON.toJSONString(mqttPacket));

        return Mono.just(mqttSession)
                .filterWhen(mqttSession0 -> ChannelCommon.containsChannel(mqttSession0.getChannelId()))
                .flatMap(ChannelCommon::getChannel)
                .map(channel -> channel.writeAndFlush(publishMessage))
                .then();
    }

    public Mono<Void> sendQos1(MqttSession mqttSession, MqttSubscribe mqttSubscribe, MqttPacket mqttPacket) {
        log.debug("===qos1 -发布- clientId: {}, mqttMessage: {}", mqttSubscribe.getClientId(), JSON.toJSONString(mqttPacket));
        return dupMessageService.putMapCache(new DupPublishMessage(mqttPacket))
                .then(Mono.just(mqttSession))
                .filterWhen(mqttSession0 -> ChannelCommon.containsChannel(mqttSession0.getChannelId()))
                .flatMap(ChannelCommon::getChannel)
                .map(channel -> channel.writeAndFlush(responseFactory(mqttPacket)))
                .then();
    }

    public Mono<Void> sendQos2(MqttSession mqttSession, MqttSubscribe mqttSubscribe, MqttPacket mqttPacket) {
        log.debug("===Qos2-发布 - clientId: {}, mqttMessage: {}", mqttSubscribe.getClientId(), JSON.toJSONString(mqttPacket));
        return dupMessageService.putMapCache(new DupPublishMessage(mqttPacket))
                .then(Mono.just(mqttSession))
                .filterWhen(mqttSession0 -> ChannelCommon.containsChannel(mqttSession0.getChannelId()))
                .flatMap(ChannelCommon::getChannel)
                .map(channel -> channel.writeAndFlush(responseFactory(mqttPacket)))
                .then();
    }

    /**
     * qos0 发布确认
     */
    private Mono<Void> sendPubAckMessage(Channel channel, int packetId) {
        MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(packetId), null);
        return Mono.just(pubAckMessage)
                .map(channel::writeAndFlush)
                .then();
    }

    /**
     * 告诉客户端表示发布已接受 qos2 第一步
     */
    private Mono<Void> sendPubRecMessage(Channel channel, int packetId) {
        MqttMessage pubRecMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(packetId <= 0 ? 1 : packetId), null);
        return Mono.just(pubRecMessage)
                .map(channel::writeAndFlush)
                .then();
    }

    /**
     * 构建响应
     */
    public MqttPublishMessage responseFactory(MqttPacket mqttPacket) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, mqttPacket.isDup(), mqttPacket.getRespQoS(), mqttPacket.isRetain(), 0),
                new MqttPublishVariableHeader(mqttPacket.getTopic(), mqttPacket.getPacketId()), Unpooled.buffer().writeBytes(mqttPacket.getMessageBytes()));
    }
}
