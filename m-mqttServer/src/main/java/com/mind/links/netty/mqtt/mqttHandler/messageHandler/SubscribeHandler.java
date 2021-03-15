package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import cn.hutool.core.util.StrUtil;
import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.cache.MsgCache;
import com.mind.links.netty.mqtt.mqttStore.service.impl.RetainMessageServiceImpl;
import entity.MqttSubscribe;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import entity.RetainMessage;
import exception.LinksExceptionTcp;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;

/**
 * date: 2021-01-14 15:11
 * description
 *
 * @author qiDing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeHandler implements IMqttMessageHandler {

    private final SubscribeServiceImpl subscribeService;

    private final RetainMessageServiceImpl retainMessageService;

    private final MsgCache msgCache;

    @Override
    public Mono<Channel> subscribe(Channel channel, MqttMessage msg) {
        return this.processSubscribe(channel, (MqttSubscribeMessage) msg);
    }

    public Mono<Channel> processSubscribe(Channel channel, MqttSubscribeMessage msg) {
        List<MqttTopicSubscription> topicSubscriptions = msg.payload().topicSubscriptions();
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        List<Integer> qosList = new ArrayList<>();
        return Flux.fromIterable(topicSubscriptions)
                .filter(this::validTopicFilter)
                .switchIfEmpty(LinksExceptionTcp.errors("错误topic"))
                .flatMap(tSub -> Mono.just(qosList.add(tSub.qualityOfService().value()))
                        .then(Mono.just(tSub)))
                .flatMap(topicSubscription -> subscribeService
                        .saveSub(new MqttSubscribe(clientId, topicSubscription.topicName(), topicSubscription.qualityOfService().value()))
                        .then(Mono.just(topicSubscription)))
                .flatMap(tSub -> Mono.just(tSub)
                        .map(b -> (MqttSubAckMessage) MqttMessageFactory.newMessage(new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(msg.variableHeader().messageId()), new MqttSubAckPayload(qosList)))
                        .map(channel::writeAndFlush)
                        .then(Mono.just(tSub))
                )
                .flatMap(tSub -> sendRetainMessage(tSub, channel,clientId))
                .then(Mono.just(channel));
    }

    /**
     * 发送缓存消息
     */
    private Mono<Void> sendRetainMessage(MqttTopicSubscription tSub, Channel channel,String clientId) {
        return Mono.just(tSub)
                .flatMap(m -> retainMessageService.getMapCache(m.topicName()))
                .flatMap(retainMessage -> {
                    MqttQoS respQoS = retainMessage.getMqttQoS() > tSub.qualityOfService().value() ? tSub.qualityOfService() : MqttQoS.valueOf(retainMessage.getMqttQoS());
                    if (respQoS == MqttQoS.AT_MOST_ONCE) {
                        return okResponse(channel, respQoS, retainMessage, 0);
                    }
                    if (respQoS == MqttQoS.AT_LEAST_ONCE) {
                        return msgCache.getId(clientId).flatMap(pId -> okResponse(channel, respQoS, retainMessage, pId));
                    }
                    if (respQoS == MqttQoS.EXACTLY_ONCE) {
                        return msgCache.getId(clientId).flatMap(pId -> okResponse(channel, respQoS, retainMessage, pId));
                    }
                    return Mono.empty();
                })
                .then();
    }

    /**
     * 发布响应
     */
    public Mono<Void> okResponse(Channel channel, MqttQoS respQoS, RetainMessage retainMessage, int packetId) {
        MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, respQoS, false, 0),
                new MqttPublishVariableHeader(retainMessage.getTopic(), packetId), Unpooled.buffer().writeBytes(retainMessage.getMessageBytes()));
        log.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get(), retainMessage.getTopic(), respQoS.value());
        return Mono.from(subscriber -> channel.writeAndFlush(publishMessage)).then();
    }

    /**
     * topic校验
     */
    private boolean validTopicFilter(MqttTopicSubscription topicSubscription) {
        String topicFilter = topicSubscription.topicName();
        // 以#或+符号开头的、以/符号结尾的订阅按非法订阅处理, 这里没有参考标准协议
        if (StrUtil.startWith(topicFilter, '+') || StrUtil.endWith(topicFilter, '/')) {
            return false;
        }
        if (StrUtil.contains(topicFilter, '#')) {
            // 如果出现多个#符号的订阅按非法订阅处理
            if (StrUtil.count(topicFilter, '#') > 1) {
                return false;
            }
        }
        if (StrUtil.contains(topicFilter, '+')) {
            //如果+符号和/+字符串出现的次数不等的情况按非法订阅处理
            return StrUtil.count(topicFilter, '+') == StrUtil.count(topicFilter, "/+");
        }
        return true;
    }
}
