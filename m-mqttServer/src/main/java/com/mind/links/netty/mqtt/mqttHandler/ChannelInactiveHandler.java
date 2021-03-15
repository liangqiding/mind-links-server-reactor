package com.mind.links.netty.mqtt.mqttHandler;

import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.common.MqttMsgTypeEnum;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import entity.MqttSession;
import exception.LinksExceptionTcp;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * description: 连接中断管理
 *
 * @author qiDing
 * date: 2021-01-02 15:27
 * @version v1.0.0
 */
@Component
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class ChannelInactiveHandler extends ChannelInboundHandlerAdapter {

    private final SessionServiceImpl sessionService;

    private final SubscribeServiceImpl subscribeService;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String clientId = (String) ctx.channel().attr(AttributeKey.valueOf("clientId")).get();
        log.warn("===频道无效channelInactive:" + clientId);
        sendWillMessage(ctx.channel(), clientId)
                .flatMap(channel -> remove(ctx.channel(), clientId))
                .flatMap(channel -> ChannelCommon.removeChannel(ctx))
                .onErrorResume(LinksExceptionTcp::errors)
                .subscribe(aBoolean -> log.warn("===遗嘱发送==="));
    }

    /**
     * 遗嘱
     */
    public Mono<Channel> sendWillMessage(Channel channel, String clientId) {
        return sessionService
                .getMqttSession(clientId)
                .filter(MqttSession::isWillFlag)
                .flatMap(mqttSession -> MqttMsgTypeEnum.PUBLISH.msgHandler(channel, mqttSession.getWillMessage()))
                .defaultIfEmpty(channel);
    }

    /**
     * 移除连接，包括订阅的topic
     */
    public Mono<Channel> remove(Channel channel, String clientId) {
        return Mono.just(clientId)
                .flatMap(b -> sessionService.remove(clientId))
                .flatMap(b -> subscribeService.removeForClient(clientId))
                .then(Mono.just(channel));
    }


}
