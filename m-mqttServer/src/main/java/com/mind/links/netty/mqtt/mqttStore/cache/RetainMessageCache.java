package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.alibaba.fastjson.JSON;
import entity.RetainMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;


/**
 * 通常，如果发布者向主题发布消息，并且没有人订阅该主题，则该消息将被代理放弃。
 * <p>
 * 但是，发布者可以通过设置保留的消息标志来告诉代理保留该主题的最后一条消息。
 *
 * @author qiding
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class RetainMessageCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Desc("topic消息缓存key")
    private final String TOPIC_MSG_MAP ="T-MAP-RETAIN";

    @Desc("保存时间 单位秒")
    private static final Long TIME = 60L;

    public Mono<Boolean> putMapCache(RetainMessage rm) {
        return redissonReactiveClient.getMapCache(TOPIC_MSG_MAP)
                .fastPut(rm.getTopic(), rm, TIME, TimeUnit.SECONDS);
    }

    public Mono<RetainMessage> getMapCache(String topic) {
        return redissonReactiveClient.getMapCache(TOPIC_MSG_MAP)
                .get(topic)
                .map(o -> JSON.parseObject(JSON.toJSONString(o), RetainMessage.class));
    }

    public Mono<Boolean> containsKey(String topic) {
        return redissonReactiveClient.getMapCache(TOPIC_MSG_MAP)
                .containsKey(topic);
    }

    public Mono<Boolean> removeMapCache(String topic) {
        return redissonReactiveClient.getMapCache(TOPIC_MSG_MAP)
                .remove(topic)
                .then(Mono.just(true));
    }

}
