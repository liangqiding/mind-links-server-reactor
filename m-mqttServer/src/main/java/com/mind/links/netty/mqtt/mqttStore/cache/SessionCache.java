package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import entity.MqttSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import utils.SessionUtil;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * date: 2021-01-21 08:51
 * description
 *
 * @author qiDing
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class SessionCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Value("${links.mqtt.broker.id:}")
    private String brokerId;

    @Desc("生成定制的redis key")
    private final String CLIENT_SESSION = "C-SESSION";

    private final Supplier<String> BROKER_SESSION = () -> brokerId + ":C-SESSION";

    @Desc("MqttSession普通序列化无法转换成json，这里我们使用自己定义的工具类(序列化)")
    private final Function<MqttSession, JSONObject> TO_JSON = SessionUtil::transPublishToJson;

    @Desc("MqttSession普通序列化无法转换成json，这里我们使用自己定义的工具类(反序列化)")
    private final Function<JSONObject, MqttSession> TO_MQTT_SESSION = SessionUtil::transToMqttSession;

    /**
     * 保存broker与客户端的关系，用于单独销毁单机缓存
     */
    public Mono<Boolean> putBrokerSession(String clientId, int expire) {
        return redissonReactiveClient.getSetCache(BROKER_SESSION.get()).add(clientId, expire, TimeUnit.SECONDS);
    }


    public Mono<Boolean> putMapCache(MqttSession mqttSession, int expire) {
        log.debug("保存session,有效期" + expire + "秒:" + TO_JSON.apply(mqttSession));
        return redissonReactiveClient.getMapCache(CLIENT_SESSION)
                .fastPut(mqttSession.getClientId(), TO_JSON.apply(mqttSession), expire, TimeUnit.SECONDS)
                .flatMap(b -> putBrokerSession(mqttSession.getClientId(), expire));
    }

    public Mono<Boolean> putMapCache(Object o, String clientId, int expire) {
        return redissonReactiveClient.getMapCache(CLIENT_SESSION)
                .fastPut(clientId, o, expire, TimeUnit.SECONDS);
    }

    public Mono<Boolean> expire(String clientId, int expire) {
        log.debug("expire:" + clientId + ",续期时长：" + expire);
        return redissonReactiveClient
                .getMapCache(CLIENT_SESSION)
                .get(clientId)
                .flatMap(o -> this.putMapCache(o, clientId, expire))
                .flatMap(b -> putBrokerSession(clientId, expire));
    }

    public Mono<MqttSession> getMapCache(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_SESSION)
                .get(clientId)
                .map(o -> JSON.parseObject(JSON.toJSONString(o)))
                .map(TO_MQTT_SESSION);
    }

    public Mono<Boolean> containsKey(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_SESSION)
                .containsKey(clientId)
                .doOnNext(b -> log.debug("===containsKey=={}:{}", clientId, b));
    }

    public Mono<Boolean> remove(String clientId) {
        return redissonReactiveClient.getMapCache(CLIENT_SESSION)
                .remove(clientId)
                .then(Mono.just(true));
    }

    public Flux<String> listClientByBroker() {
        return redissonReactiveClient.getSetCache(BROKER_SESSION.get()).iterator().map(Object::toString);
    }
    public Mono<Integer> sessionCount() {
        return redissonReactiveClient.getMapCache(CLIENT_SESSION).size().doOnNext(integer -> System.out.println("=="+integer));
    }
}
