package com.mind.links.netty.mqtt.mqttStore.cache;

import annotation.Desc;
import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import utils.LinksSnowflakeUtils;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.function.UnaryOperator;

/**
 * date: 2021-01-18 16:16
 * description 服务器退出阶段进行重置
 *
 * @author qiDing
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class MsgCache {

    private final RedissonReactiveClient redissonReactiveClient;

    @Value("${links.mqtt.broker.id:}")
    private String brokerId;

    @Desc("缓存key生成")
    private final UnaryOperator<String> MSG_ID = key -> brokerId + ":MSG_ID:" + key;

    /**
     * 消息ID在每个客户端和每个方向的基础上进行处理.也就是说,代理将为每个连接的客户端的每个传出消息创建消息ID,
     * 并且这些消息ID将完全独立于用于发布给其他客户端的同一消息的任何其他消息ID.同样,每个客户端都会为其发送的
     * 消息生成自己的消息ID.
     * <p>
     * 思路：int16 最大值为 %65536 ，当达到上限2的31次方-1，重置redis的id，每次服务器退出，在销毁阶段清空当前服务器下的所有id
     */
    public Mono<Integer> getId(String clientId) {
        return redissonReactiveClient
                .getAtomicLong(MSG_ID.apply(clientId))
                .addAndGet(1)
                .filter(id -> id < (2 << 30) - 1)
                .switchIfEmpty(redissonReactiveClient.getAtomicLong(MSG_ID.apply(clientId)).getAndSet(1))
                .map(userId -> userId % 65536)
                .map(Math::toIntExact);
    }


    /**
     * 截取雪花id左5位，作为消息id足够了,mqtt消息id允许重复，这种获取方法对性能有一定的提升
     */
    public Integer genSnowflakeId() {
        String idL = String.valueOf(LinksSnowflakeUtils.genId());
        idL = idL.substring(idL.length() - 5);
        return Integer.valueOf(idL);
    }

    /**
     * 随机id
     */
    public Integer getRandomId() {
        Random random = new Random();
        return random.nextInt(65536);
    }
}
