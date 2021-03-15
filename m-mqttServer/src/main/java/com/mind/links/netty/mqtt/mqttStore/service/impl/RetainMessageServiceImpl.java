package com.mind.links.netty.mqtt.mqttStore.service.impl;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.mqttStore.cache.RetainMessageCache;
import com.mind.links.netty.mqtt.mqttStore.service.IRetainMessageService;
import entity.RetainMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;


/**
 * date: 2021-01-18 16:05
 * description
 *
 * @author qiDing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetainMessageServiceImpl implements IRetainMessageService {

    private final RetainMessageCache retainMessageCache;

    @Override
    public Mono<Boolean> putMapCache(RetainMessage rm) {
        return retainMessageCache.putMapCache(rm);
    }

    @Override
    public Mono<RetainMessage> getMapCache(String topic) {
        return retainMessageCache.getMapCache(topic);
    }

    @Override
    public Mono<Boolean> containsKey(String topic) {
        return retainMessageCache.containsKey(topic);
    }

    @Override
    public Mono<Boolean> removeMapCache(String topic) {
        return retainMessageCache.removeMapCache(topic);
    }

}
