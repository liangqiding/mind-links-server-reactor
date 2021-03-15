package com.mind.links.netty.mqtt.mqttStore.service.impl;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.mqttStore.cache.DupMessageCache;
import com.mind.links.netty.mqtt.mqttStore.service.IDupMessageService;
import com.mind.links.netty.mqtt.mqttStore.service.IDupRelService;
import entity.DupPublishMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * date: 2021-01-19 15:53
 * description
 *
 * @author qiDing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DupMessageServiceImpl implements IDupMessageService {

    private final DupMessageCache dupMessageCache;

    @Override
    public Mono<Boolean> putMapCache(DupPublishMessage dup) {
        return dupMessageCache.putMapCache(dup).onErrorReturn(false);
    }

    @Override
    public Mono<DupPublishMessage> getMapCache(String clientId, int packetId) {
        return dupMessageCache.getMapCache(clientId, packetId);
    }

    @Override
    public Mono<Boolean> containsKey(String clientId, int packetId) {
        return dupMessageCache.containsKey(clientId, packetId);
    }

    @Override
    public Mono<Boolean> removeMapCache(String clientId, int packetId) {
        return dupMessageCache.removeMapCache(clientId, packetId);
    }
    @Override
    public Mono<Boolean> removeMapCacheByClient(String clientId) {
        return dupMessageCache.removeMapCacheByClient(clientId);
    }

    @Override
    public Flux<DupPublishMessage> listMapCacheByClientId(String clientId) {
        return dupMessageCache.listMapCacheByClientId(clientId);
    }
}
