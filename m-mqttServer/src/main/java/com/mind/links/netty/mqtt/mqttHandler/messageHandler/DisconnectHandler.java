package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupMessageServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.DupRelServiceImpl;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 16:15
 * description 断开连接
 *
 * @author qiDing
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DisconnectHandler implements IMqttMessageHandler {

    private final DupMessageServiceImpl dupMessageService;

    private final DupRelServiceImpl dupRelService;

    private final SessionServiceImpl sessionService;

    @Override
    public Mono<Channel> disconnect(Channel channel) {
        log.debug("断开连接:" + channel);
        return this.remove(channel);
    }

    public Mono<Channel> remove(Channel channel) {
        final String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
        return Mono.just(clientId)
                .flatMap(s -> dupMessageService.removeMapCacheByClient(clientId))
                .flatMap(b -> dupRelService.removeMapByClient(clientId))
                .flatMap(b -> sessionService.remove(clientId))
                .then(Mono.just(channel));
    }
}
