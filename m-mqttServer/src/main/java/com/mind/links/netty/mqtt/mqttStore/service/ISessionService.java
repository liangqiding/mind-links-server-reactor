package com.mind.links.netty.mqtt.mqttStore.service;

import entity.MqttSession;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 会话存储服务接口
 *
 * @author qiding
 */
public interface ISessionService {

    /**
     * 清空缓存会话 如果会话中已存储这个新连接的clientId, 就关闭之前该clientId的连接
     *
     * @param msg Mqtt连接消息
     */
    Mono<Void> cleanSession(MqttConnectMessage msg);


    /**
     * 缓存会话
     *
     * @param mqttSession session
     * @param expire      有效期
     * @return Void
     */
    Mono<Boolean> saveMqttSession(MqttSession mqttSession, int expire);

    /**
     * 设置会话失效时间
     *
     * @param clientId 客户端id
     * @param expire   有效期
     */
    Mono<Boolean> expire(String clientId, int expire);

    /**
     * 获取会话
     *
     * @param clientId 客户端id
     * @return mqttSession
     */
    Mono<MqttSession> getMqttSession(String clientId);

    /**
     * 判断会话是否存在
     *
     * @param clientId clientId的会话是否存在
     * @return true or false
     */
    Mono<Boolean> containsKey(String clientId);


    /**
     * 删除会话
     *
     * @param clientId clientId
     */
    Mono<Boolean> remove(String clientId);


    /**
     * 查询该broker 下的所有用户
     *
     * @return session
     */
    Flux<String> listSessionByBroker();

    /**
     * 统计当前连接总数
     *
     * @return count
     */
    Mono<Integer> sessionCount();
}
