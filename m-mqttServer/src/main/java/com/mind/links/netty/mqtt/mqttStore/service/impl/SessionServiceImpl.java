package com.mind.links.netty.mqtt.mqttStore.service.impl;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import com.mind.links.netty.mqtt.mqttStore.cache.SessionCache;
import com.mind.links.netty.mqtt.mqttStore.service.ISessionService;
import entity.MqttSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * 会话存储服务
 *
 * @author qiding
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements ISessionService {

    private final SubscribeServiceImpl subscribeService;

    private final SessionCache sessionCache;


    public Mono<Void> extendSession(Channel channel) {
        String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        return Mono
                .just(clientId)
                .filterWhen(this::containsKey)
                .flatMap(this::getMqttSession)
                .filterWhen(mqttSession -> ChannelCommon.containsChannel(mqttSession.getChannelId()))
                .flatMap(mqttSession -> sessionCache.expire(clientId, mqttSession.getExpire()))
                .then();
    }

    @Override
    public Mono<Void> cleanSession(MqttConnectMessage msg) {
        String clientId = msg.payload().clientIdentifier();
        return Mono.just(clientId)
                // 清理缓存 和缓存的消息
                .filterWhen(this::containsKey)
                .flatMap(this::getMqttSession)
                .flatMap(this::doCleanSession)
                .flatMap(mqttSession -> ChannelCommon.removeChannel(mqttSession.getChannelId()))
                //如果不存在session，则清除之前的其他缓存
                .switchIfEmpty(subscribeService.removeForClient(msg.payload().clientIdentifier()));
    }


    /**
     * 清除旧缓存 消息缓存
     */
    private Mono<MqttSession> doCleanSession(MqttSession mqttSession) {
        log.debug("清除旧缓存:" + JSON.toJSONString(mqttSession));
        return Mono.just(mqttSession)
                .filter(MqttSession::isCleanSession)
                .doOnNext(m -> sessionCache.remove(m.getClientId()))
                .defaultIfEmpty(mqttSession);
    }


    @Override
    public Mono<Boolean> saveMqttSession(MqttSession mqttSession, int expire) {
        log.debug("保存session,有效期" + expire + "秒:" + mqttSession.getClientId());
        return sessionCache.putMapCache(mqttSession, expire);
    }

    @Override
    public Mono<Boolean> expire(String clientId, int expire) {
        log.debug("expire:" + clientId + ",续期时长：" + expire);
        return sessionCache.expire(clientId, expire);
    }

    @Override
    public Mono<MqttSession> getMqttSession(String clientId) {
        return sessionCache.getMapCache(clientId);
    }

    @Override
    public Mono<Boolean> containsKey(String clientId) {
        return sessionCache.containsKey(clientId);
    }


    @Override
    public Mono<Boolean> remove(String clientId) {
        return sessionCache.remove(clientId);
    }

    @Override
    public Flux<String> listSessionByBroker() {
        return sessionCache.listClientByBroker();
    }

    @Override
    public Mono<Integer> sessionCount() {
        return sessionCache.sessionCount();
    }

}
