package com.mind.links.netty.mqtt.mqttStore.cache;

import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-22 09:24
 * description 通用缓存清空  销毁清空
 *
 * @author qiDing
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DestroyCache {

    @Value("${links.mqtt.broker.id:}")
    private String brokerId;

    private final RedissonReactiveClient redissonReactiveClient;

    private final SessionServiceImpl sessionService;

    private final SubscribeServiceImpl subscribeService;

    /**
     * 销毁与该客户端有关的所有缓存包含用户session和用户订阅的topic
     */
    public Mono<Void> deleteAllCacheForBroker() {
        return sessionService.listSessionByBroker()
                .flatMap(c -> subscribeService.removeForClient(c).then(Mono.just(c)))
                .flatMap(sessionService::remove)
                .flatMap(o -> redissonReactiveClient.getKeys().deleteByPattern(brokerId + "*"))
                .then();
    }
}
