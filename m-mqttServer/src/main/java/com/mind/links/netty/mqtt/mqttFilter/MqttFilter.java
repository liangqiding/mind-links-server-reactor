package com.mind.links.netty.mqtt.mqttFilter;

import cn.hutool.core.util.StrUtil;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import exception.LinksExceptionTcp;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * date: 2021-01-12 15:23
 * description  过滤器核心类
 *
 * @author qiDing
 */
@Component
@RequiredArgsConstructor
public class MqttFilter implements IMqttFilter {

    private final BrokerProperties brokerProperties;

    private final SessionServiceImpl sessionServiceImpl;

    @Override
    public Mono<Channel> filter(final Channel channel, final MqttConnectMessage msg) {
        return Mono.just(channel)
                .doFirst(() -> this.sessionServiceImpl.cleanSession(msg))
                .filterWhen(c -> this.isDecoderSuccess(c, msg))
                .switchIfEmpty(LinksExceptionTcp.errors("解码器故障"))
                .filter(c -> this.clientIdIsNotNull(c, msg))
                .switchIfEmpty(LinksExceptionTcp.errors("客户端id为空"))
                .filter(c -> this.isAuthSuccess(c, msg))
                .switchIfEmpty(LinksExceptionTcp.errors("账号密码认证错误"));
    }

    @Override
    public Mono<Boolean> isDecoderSuccess(Channel channel, MqttConnectMessage msg) {
        return Mono.just(msg)
                .map(message -> {
                    if (message.decoderResult().isFailure()) {
                        Throwable cause = message.decoderResult().cause();
                        if (cause instanceof MqttUnacceptableProtocolVersionException) {
                            // 不支持的协议版本
                            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false), null);
                            channel.writeAndFlush(connAckMessage);
                            channel.close();
                        } else if (cause instanceof MqttIdentifierRejectedException) {
                            // 不合格的clientId
                            MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
                            channel.writeAndFlush(connAckMessage);
                        }
                        return false;
                    }
                    return true;
                });
    }

    @Override
    public boolean clientIdIsNotNull(Channel channel, MqttConnectMessage msg) {
        return Optional.of(StrUtil.isBlank(msg.payload().clientIdentifier()))
                .filter(Boolean::booleanValue)
                .map(aBoolean -> {
                    MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
                    channel.writeAndFlush(connAckMessage);
                    return false;
                }).orElse(true);

    }

    @Override
    public boolean isAuthSuccess(Channel channel, MqttConnectMessage msg) {
        if (brokerProperties.getMqttPasswordMust()) {
            String username = msg.payload().userName();
            String password = msg.payload().passwordInBytes() == null ? null : new String(msg.payload().passwordInBytes(), CharsetUtil.UTF_8);
            // 自行添加认证逻辑
            if (true) {
                MqttConnAckMessage connAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false), null);
                channel.writeAndFlush(connAckMessage);
                channel.close();
                return false;
            }
        }
        return true;
    }


}
