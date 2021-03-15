package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.alibaba.fastjson.JSON;
import entity.DupPublishMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/**
 * 消息重发缓存 当QoS=1和QoS=2时存在该重发机制
 *
 * @author qiding
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DupMessageCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Desc("缓存key生成")
    private final UnaryOperator<String> CLIENT_MAP_DUP = key -> ":C-MAP-DUP:" + key;

    @Desc("保存时间 单位秒")
    private static final Long TIME = 60 * 60L;

    public Mono<Boolean> putMapCache(DupPublishMessage dup) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(dup.getClientId()))
                .fastPut(dup.getPacketId(), dup, TIME, TimeUnit.SECONDS);
    }

    public Mono<DupPublishMessage> getMapCache(String clientId, int packetId) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(clientId)).get(packetId)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), DupPublishMessage.class));
    }

    public Mono<Boolean> containsKey(String clientId, int packetId) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(clientId))
                .containsKey(packetId);
    }

    public Mono<Boolean> removeMapCache(String clientId, int packetId) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(clientId))
                .remove(packetId).then(Mono.just(true));
    }

    public Mono<Boolean> removeMapCacheByClient(String clientId) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(clientId))
                .delete();
    }

    public Flux<DupPublishMessage> listMapCacheByClientId(String clientId) {
        return redissonReactiveClient
                .getMapCache(CLIENT_MAP_DUP.apply(clientId))
                .entryIterator()
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), DupPublishMessage.class));
    }
}
