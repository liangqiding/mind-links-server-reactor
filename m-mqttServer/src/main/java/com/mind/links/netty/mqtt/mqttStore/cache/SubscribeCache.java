package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import entity.MqttSubscribe;
import exception.LinksExceptionTcp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 通配符topic缓存设计
 *
 * @author qiding
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscribeCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Desc("缓存key生成：（用于通过客户id取其订阅的topic）")
    private final UnaryOperator<String> CLIENT_TOPIC_MAP = key -> "C-T-SUB:" + key;

    @Desc("缓存key生成：（用于通过topic取所有订户）")
    private final UnaryOperator<String> TOPIC_CLIENT_MAP = key -> "T-C-SUB:" + key;

    @Desc("缓存key生成：（用于通过topic取所有订户（通配模式））")
    private final UnaryOperator<String> TOPIC_CLIENT_WILDCARD_MAP = key -> "TW-C-SUB:" + key;

    @Desc("是否（通配模式）")
    private static final Predicate<String> IF_WILDCARD = topic -> topic.contains("#") || topic.contains("+");


    public Mono<Boolean> saveSub(MqttSubscribe mqttSubscribe) {
        return this.saveSub(mqttSubscribe, IF_WILDCARD.test(mqttSubscribe.getTopic()));
    }

    public Mono<Boolean> saveSub(MqttSubscribe mqttSubscribe, boolean wildcard) {
        return Mono.just(mqttSubscribe)
                .flatMap(b -> redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(mqttSubscribe.getClientId()))
                        .fastPut(mqttSubscribe.getTopic(), mqttSubscribe))
                .switchIfEmpty(LinksExceptionTcp.errors("session保存失败,检查redisson方面的配置"))
                .flatMap(b -> redissonReactiveClient.getMapCache(wildcard ? TOPIC_CLIENT_WILDCARD_MAP.apply(mqttSubscribe.getTopic()) : TOPIC_CLIENT_MAP.apply(mqttSubscribe.getTopic()))
                        .fastPut(mqttSubscribe.getClientId(), mqttSubscribe))
                .doOnNext(o -> log.debug("===saveSub:" + mqttSubscribe));
    }

    public Mono<MqttSubscribe> getSub(String clientId, String topic) {
        return redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(clientId)).get(topic).map(o -> JSON.parseObject(JSON.toJSONString(o), MqttSubscribe.class));
    }


    public Mono<Boolean> containsKey(String topic, String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(clientId)).containsKey(topic);
    }

    public Mono<Boolean> removeOne(String clientId, String topic) {
        return this.removeOne(clientId, topic, IF_WILDCARD.test(topic));
    }

    public Mono<Boolean> removeOne(String clientId, String topic, boolean wildcard) {
        return Mono.just(false)
                .then(redissonReactiveClient.getMapCache(wildcard ? TOPIC_CLIENT_WILDCARD_MAP.apply(topic) : TOPIC_CLIENT_MAP.apply(topic)).remove(clientId))
                .then(redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(clientId)).remove(topic))
                .then(Mono.just(true))
                .doOnNext(aBoolean -> log.debug("===removeOne=={}::{}", clientId, topic));
    }

    public Mono<Void> removeForClient(String clientId) {
        return this.listSubByClient(clientId)
                .flatMap(mqttSubscribe -> redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(mqttSubscribe.getClientId())).delete()
                        .then(Mono.just(mqttSubscribe)))
                .flatMap(mqttSubscribe -> redissonReactiveClient.getMapCache(TOPIC_CLIENT_MAP.apply(mqttSubscribe.getTopic())).remove(clientId)
                        .then(Mono.just(mqttSubscribe)))
                .flatMap(mqttSubscribe -> redissonReactiveClient.getMapCache(TOPIC_CLIENT_WILDCARD_MAP.apply(mqttSubscribe.getTopic())).delete()
                        .then(Mono.empty()))
                .then();

    }

    public Flux<MqttSubscribe> listSubAll() {
        return redissonReactiveClient.getKeys().getKeysByPattern(CLIENT_TOPIC_MAP.apply("*"))
                .flatMap(key -> redissonReactiveClient.getMapCache(key).entryIterator())
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), MqttSubscribe.class))
                .doOnNext(o -> log.debug("===listSubAll:" + JSON.toJSONString(o)));
    }

    public Flux<MqttSubscribe> listSubByTopic(String topic) {
        return this.listSubByTopic(topic, false);
    }

    public Flux<MqttSubscribe> listSubByTopic(String topic, boolean wildcard) {
        return redissonReactiveClient.getMapCache(wildcard ? TOPIC_CLIENT_WILDCARD_MAP.apply(topic) : TOPIC_CLIENT_MAP.apply(topic))
                .entryIterator()
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), MqttSubscribe.class))
                .doOnNext(o -> log.debug("===listSubByTopic--{}", o));
    }

    public Flux<MqttSubscribe> listAllWildcardSub() {
        return redissonReactiveClient.getKeys()
                .getKeysByPattern("*:TW-C-SUB:*")
                .flatMap(key -> redissonReactiveClient.getMapCache(key).entryIterator())
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), MqttSubscribe.class))
                .doOnNext(o -> log.debug("===listAllWildcardSub:" + o));
    }

    public Flux<MqttSubscribe> listSubByClient(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_TOPIC_MAP.apply(clientId))
                .entryIterator()
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), MqttSubscribe.class))
                .doOnNext(o -> log.debug("===listSubByClient:" + JSON.toJSONString(o)));
    }
}
