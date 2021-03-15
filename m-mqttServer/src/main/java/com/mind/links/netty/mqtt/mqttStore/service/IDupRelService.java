package com.mind.links.netty.mqtt.mqttStore.service;

import entity.DupPubRelMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author qidingliang
 */
public interface IDupRelService {

    /**
     * qos2 发布存储
     *
     * @param dpr qos2 发布存储
     * @return Boolean
     */
    Mono<Boolean> putMapCache(DupPubRelMessage dpr);


    /**
     * 是否存在
     *
     * @param clientId 客户id
     * @param packetId 消息id
     * @return Boolean
     */
    Mono<Boolean> containsKey(String clientId, int packetId);


    /**
     * 移除
     *
     * @param clientId 客户id
     * @param packetId 消息id
     * @return Boolean
     */
    Mono<Boolean> removeMapKey(String clientId, int packetId);


    /**
     * 移除该客户端下的所有
     *
     * @param clientId 客户id
     * @return Boolean
     */
    Mono<Boolean> removeMapByClient(String clientId);


    /**
     * 获取该客户端下的所有
     *
     * @param clientId 客户id
     * @return 发布保存的qos2
     */
    Flux<DupPubRelMessage> listMapByClientId(String clientId);

}
