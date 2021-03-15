package com.mind.links.netty.mqtt.mqttStore.service;

import entity.RetainMessage;
import reactor.core.publisher.Mono;

/**
 * @author qidingliang
 */
public interface IRetainMessageService {

    /**
     * Retain==1 保留消息
     *
     * @param rm 保存的消息
     * @return boolean
     */
    Mono<Boolean> putMapCache(RetainMessage rm);

    /**
     * 获取保留的消息
     *
     * @param topic 主题
     * @return 保留的消息
     */
    Mono<RetainMessage> getMapCache(String topic);

    /**
     * 是否存在
     *
     * @param topic 主题
     * @return boolean
     */
    Mono<Boolean> containsKey(String topic);


    /**
     * 移除保留的消息
     *
     * @param topic 主题
     * @return boolean
     */
    Mono<Boolean> removeMapCache(String topic);
}
