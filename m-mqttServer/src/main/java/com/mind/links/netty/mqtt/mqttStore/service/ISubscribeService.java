package com.mind.links.netty.mqtt.mqttStore.service;

import entity.MqttSubscribe;
import reactor.core.publisher.Flux;

/**
 * date: 2021-02-01 14:27
 * description
 *
 * @author qiDing
 */
public interface ISubscribeService {

    /**
     * 匹配（通配）及（非通配）的topic总数
     *
     * @param topic 主题
     * @return MqttSubscribe
     */
    Flux<MqttSubscribe> matchTopic(String topic);


    /**
     * 通配思路： 先把原topic->按通配wildcardTopic格式转换->再与通配的wildcardTopic对比
     *
     * @param topic  主题
     * @param wildcardTopic 通配topic
     * @return boolean
     */
    boolean ifWildcardTopic(String topic, String wildcardTopic);
}
