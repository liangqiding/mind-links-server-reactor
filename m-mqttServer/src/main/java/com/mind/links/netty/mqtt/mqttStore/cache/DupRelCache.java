package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.common.ChannelCommon;
import entity.DupPubRelMessage;
import entity.DupPublishMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * date: 2021-01-19 16:55
 * description
 *
 * @author qiDing
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class DupRelCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Value("${links.mqtt.broker.id:}")
    private String brokerId;

    @Desc("缓存key生成")
    private  final UnaryOperator<String> CLIENT_MAP_DUP_REL = key -> brokerId + ":C-MAP-DUP_REL:" + key;

    @Desc("保存时间 单位秒")
    private  final Long TIME = 60 * 60L;

    public Mono<Boolean> putMapCache(DupPubRelMessage dupR) {
        return redissonReactiveClient.getMapCache(CLIENT_MAP_DUP_REL.apply(dupR.getClientId())).fastPut(dupR.getPacketId(), 0);
    }


    public Mono<Boolean> containsKey(String clientId, int packetId) {
        return redissonReactiveClient.getMapCache(CLIENT_MAP_DUP_REL.apply(clientId)).containsKey(packetId);
    }

    public Mono<Boolean> removeMapKey(String clientId, int packetId) {
        return redissonReactiveClient.getMapCache(CLIENT_MAP_DUP_REL.apply(clientId)).remove(packetId).then(Mono.just(true));
    }

    public Mono<Boolean> removeMapByClient(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_MAP_DUP_REL.apply(clientId)).delete();
    }

    public Flux<DupPubRelMessage> listMapByClientId(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_MAP_DUP_REL.apply(clientId))
                .entryIterator()
                .map(Map.Entry::getValue)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), DupPubRelMessage.class));
    }

}
