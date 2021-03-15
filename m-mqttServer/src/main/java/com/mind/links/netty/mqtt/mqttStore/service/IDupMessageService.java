package com.mind.links.netty.mqtt.mqttStore.service;

import entity.DupPublishMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author qidingliang
 */
public interface IDupMessageService {
    /**
     * qos1 2 存储消息
     *
     * @param dup 存储消息 qos1 2
     * @return boolean
     */
    Mono<Boolean> putMapCache(DupPublishMessage dup);


    /**
     * 获取存储的消息
     *
     * @param clientId 客户id
     * @param packetId 消息id
     * @return 存储的消息
     */
    Mono<DupPublishMessage> getMapCache(String clientId, int packetId);


    /**
     * 是否存在
     *
     * @param clientId 客户id
     * @param packetId 消息id
     * @return boolean
     */
    Mono<Boolean> containsKey(String clientId, int packetId);


    /**
     * 移除该消息
     *
     * @param clientId 客户id
     * @param packetId 消息id
     * @return boolean
     */
    Mono<Boolean> removeMapCache(String clientId, int packetId);

    /**
     * 移除该客户端下的所有消息
     *
     * @param clientId 客户id
     * @return boolean
     */
    Mono<Boolean> removeMapCacheByClient(String clientId);


    /**
     * 获取该客户端下所有消息
     *
     * @param clientId 客户id
     * @return 消息集合
     */
    Flux<DupPublishMessage> listMapCacheByClientId(String clientId);

}
