package com.mind.links.netty.mqtt.mqttStore.service.impl;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.mqttStore.cache.DupRelCache;
import com.mind.links.netty.mqtt.mqttStore.service.IDupRelService;
import entity.DupPubRelMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * date: 2021-01-20 08:38
 * description
 *
 * @author qiDing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DupRelServiceImpl implements IDupRelService {

    private final DupRelCache dupRelCache;

    @Override
    public Mono<Boolean> putMapCache(DupPubRelMessage dpr) {
        return dupRelCache.putMapCache(dpr);
    }

    @Override
    public Mono<Boolean> containsKey(String clientId, int packetId) {
        return dupRelCache.containsKey(clientId, packetId);
    }

    @Override
    public Mono<Boolean> removeMapKey(String clientId, int packetId) {
        return dupRelCache.removeMapKey(clientId, packetId);
    }

    @Override
    public Mono<Boolean> removeMapByClient(String clientId) {
        return dupRelCache.removeMapByClient(clientId);
    }

    @Override
    public Flux<DupPubRelMessage> listMapByClientId(String clientId) {
        return dupRelCache.listMapByClientId(clientId);
    }
}
