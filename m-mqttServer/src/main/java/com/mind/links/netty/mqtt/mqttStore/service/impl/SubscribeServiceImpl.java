package com.mind.links.netty.mqtt.mqttStore.service.impl;


import cn.hutool.core.util.StrUtil;
import com.mind.links.netty.mqtt.mqttStore.service.ISessionService;
import com.mind.links.netty.mqtt.mqttStore.service.ISubscribeService;
import entity.MqttSubscribe;
import com.mind.links.netty.mqtt.mqttStore.cache.SubscribeCache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 订阅存储服务,业务逻辑处理,预留业务扩展,日记打印等
 *
 * @author qiding
 */
@Component
@Slf4j
@AllArgsConstructor
public class SubscribeServiceImpl implements ISubscribeService {

    private final SubscribeCache subscribeCache;


    public Mono<Boolean> saveSub(MqttSubscribe mqttSubscribe) {
        return subscribeCache.saveSub(mqttSubscribe);
    }


    public Mono<Boolean> removeOne(String clientId, String topic) {
        return subscribeCache.removeOne(clientId, topic);
    }


    public Mono<Void> removeForClient(String clientId) {
        return subscribeCache.removeForClient(clientId);
    }


    @Override
    public Flux<MqttSubscribe> matchTopic(String topic) {
        List<MqttSubscribe> mqttSubscribes = Collections.synchronizedList(new ArrayList<MqttSubscribe>());
        return subscribeCache.listSubByTopic(topic)
                .map(mqttSubscribes::add)
                .then(Mono.just(true))
                .flatMapMany(b -> subscribeCache.listAllWildcardSub())
                .filter(m -> this.ifWildcardTopic(topic, m.getTopic()))
                .map(mqttSubscribes::add)
                .then(Mono.just(true))
                .flatMapIterable(b -> mqttSubscribes)
                .doOnNext(mqttSubscribe -> log.info("===matchTopic:获取到相关的topic 总数:{},:{}",mqttSubscribes.size(), mqttSubscribe.getTopic()));

    }

    @Override
    public boolean ifWildcardTopic(String topic, String wildcardTopic) {
        // 起手校验：原topic不可能比通配topic短
        if (StrUtil.split(topic, '/').size() >= StrUtil.split(wildcardTopic, '/').size()) {
            // a/b/c  原topic
            List<String> sTopic = StrUtil.split(topic, '/');
            // a/+/#  通配的topic
            List<String> wTopic = StrUtil.split(wildcardTopic, '/');
            StringBuilder newTopic = new StringBuilder();
            for (int i = 0; i < wTopic.size(); i++) {
                String w = wTopic.get(i);
                if ("+".equals(w)) {
                    newTopic.append("+/");
                } else if ("#".equals(w)) {
                    newTopic.append("#/");
                    break;
                } else {
                    newTopic.append(sTopic.get(i)).append("/");
                }
            }
            newTopic = new StringBuilder(StrUtil.removeSuffix(newTopic.toString(), "/"));
            // 最后对比topic
            return wildcardTopic.equals(newTopic.toString());

        }
        return false;
    }

}
