package com.mind.links.netty.mqtt.common;

import annotation.Desc;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import entity.MqttSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * date: 2021-01-13 16:23
 * description
 *
 * @author qiDing
 */
@Slf4j
@Component
public class ChannelCommon {

    public static BrokerProperties brokerProperties;

    @Autowired
    public void setBrokerProperties(BrokerProperties b) {
        brokerProperties = b;
    }

    @Desc("netty频道存储 k:channelId  v:channel")
    public final static ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Desc("netty频道存储 k:channelId(String)  v:channelId")
    public final static Map<String, ChannelId> CHANNEL_ID_MAP = new ConcurrentHashMap<>(2 << 4);

    @Desc("构造CHANNEL_ID_MAP的存储key")
    private final static Function<ChannelHandlerContext, String> CHANNEL_MAP_KEY = ctx -> brokerProperties.getId() + "_" + ctx.channel().id().asLongText();

    @Desc("构造CHANNEL_ID_MAP的存储key")
    private final static Function<String, String> CHANNEL_MAP_KEY0 = channelId -> brokerProperties.getId() + "_" + channelId;

    /**
     * 是否包含该Channel
     */
    public static Mono<Boolean> containsChannel(String channelId) {
        return Mono.just(channelId)
                .filter(c -> CHANNEL_ID_MAP.containsKey(CHANNEL_MAP_KEY0.apply(c)))
                .map(c -> CHANNEL_ID_MAP.get(CHANNEL_MAP_KEY0.apply(c)))
                .filter(cId -> CHANNEL_GROUP.find(cId) != null)
                .map(c -> true)
                .defaultIfEmpty(false);
    }

    public static Mono<Void> saveChannel(ChannelHandlerContext ctx) {
        log.debug("保存channel" + ctx.channel());
        return Mono.just(ctx)
                .doOnNext(context->CHANNEL_GROUP.add(context.channel()))
                .doOnNext(context->CHANNEL_ID_MAP.put(CHANNEL_MAP_KEY.apply(ctx), ctx.channel().id()))
                .then();

    }

    public static Mono<Void> removeChannel(ChannelHandlerContext ctx) {
        log.debug("移除channel" + ctx.channel());
        return Mono.just(ctx.channel())
                .doOnNext(CHANNEL_GROUP::remove)
                .doOnNext(channel -> CHANNEL_ID_MAP.remove(CHANNEL_MAP_KEY.apply(ctx)))
                .then();
    }

    public static Mono<Void> removeChannel(String channelId) {
        log.debug("移除channel" + channelId);
        return getChannelId(channelId)
                .doOnNext(c -> CHANNEL_ID_MAP.remove(CHANNEL_MAP_KEY0.apply(channelId)))
                .doOnNext(c -> removeMapByChannelId(channelId))
                .then();
    }


    public static Mono<Void> channelClose(Channel channel) {
        log.debug("channelClose" + channel);
        return Mono.just(channel.close()).then();
    }


    /**
     * 获得频道id
     */
    public static Mono<ChannelId> getChannelId(String channelId) {
        return Mono.just(channelId)
                .map(s -> CHANNEL_ID_MAP.get(CHANNEL_MAP_KEY0.apply(s)));
    }

    /**
     * 获得频道id
     */
    public Mono<ChannelId> getChannelId(ChannelHandlerContext ctx) {
        return Mono.just(ctx)
                .map(c -> CHANNEL_ID_MAP.get(CHANNEL_MAP_KEY.apply(c)));
    }

    /**
     * 获得频道
     */
    public static Mono<Channel> getChannel(ChannelId channelId) {
        return Mono.just(channelId).map(CHANNEL_GROUP::find);
    }

    /**
     * 获得频道
     */
    public static Mono<Channel> getChannel(MqttSession mqttSession) {
        return getChannel(mqttSession.getChannelId());
    }

    /**
     * 获得频道
     */
    public static Mono<Channel> getChannel(String channelId) {
        return Mono.just(channelId)
                .flatMap(ChannelCommon::getChannelId)
                .flatMap(ChannelCommon::getChannel);
    }

    public static Object getClientId(Channel channel) {
        return channel.attr(AttributeKey.valueOf("clientId")).get();
    }

    private static void removeMapByChannelId(String channelId) {
        CHANNEL_ID_MAP.remove(CHANNEL_MAP_KEY0.apply(channelId));
    }
}
