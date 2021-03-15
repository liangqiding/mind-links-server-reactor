package com.mind.links.netty.mqtt.mqttHandler.messageHandler;

import com.mind.links.netty.mqtt.mqttHandler.IMqttMessageHandler;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * date: 2021-01-14 16:13
 * description ping响应
 *
 * @author qiDing
 */
@Component
@Slf4j
public class PingRespHandler implements IMqttMessageHandler {
    @Override
    public Mono<Channel> pingResp(Channel channel) {
        log.debug("ping响应:"+channel);
        return Mono.just(channel);
    }
}
