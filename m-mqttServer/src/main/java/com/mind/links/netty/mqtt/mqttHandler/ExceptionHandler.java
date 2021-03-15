package com.mind.links.netty.mqtt.mqttHandler;


import com.mind.links.netty.mqtt.common.ChannelCommon;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SubscribeServiceImpl;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.SocketAddress;
import java.util.Optional;


/**
 * description : TODO 异常处理
 *
 * @author : qiDing
 * date: 2021-01-03 08:45
 * @version v1.0.0
 */
@Slf4j
@ChannelHandler.Sharable
@Component
@RequiredArgsConstructor
public class ExceptionHandler extends ChannelDuplexHandler {

    private final SessionServiceImpl sessionService;

    private final SubscribeServiceImpl subscribeService;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught,断开连接:", cause);
                remove(ctx.channel())
                        .flatMap(ChannelCommon::channelClose)
                        .then(Mono.just("success"))
                        .subscribe();
    }

    public Mono<Channel> remove(Channel channel) {
        final String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        return Mono.just(clientId)
                .flatMap(b -> sessionService.remove(clientId))
                .flatMap(b -> subscribeService.removeForClient(clientId))
                .then(Mono.just(channel));
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise.addListener((ChannelFutureListener) future ->
                Optional.of(future.isSuccess())
                        .filter(b -> !b)
                        .ifPresent(b -> log.error("connect exceptionCaught", future.cause()))));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise.addListener((ChannelFutureListener) future ->
                Optional.of(future.isSuccess())
                        .filter(b -> !b)
                        .ifPresent(b -> log.error("write exceptionCaught", future.cause()))));
    }
}
