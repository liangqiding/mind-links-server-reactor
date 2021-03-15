package com.mind.links.netty.mqtt.config;

import annotation.Desc;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务配置
 *
 * @author qiding
 */
@Component
@Accessors(chain = true)
@NoArgsConstructor
@ConfigurationProperties(prefix = "links.mqtt.broker")
@Data
public class BrokerProperties {

    @Desc("Broker唯一标识")
    private String id;

    @Desc("SSL启动的IP地址")
    private String host;

    @Desc("SSL端口号")
    private Integer port;

    @Desc("是否开启集群")
    private Boolean clusterEnabled;

    @Desc("WebSocket SSL端口号, 默认9995端口")
    private Integer webSocketPort;

    @Desc("WebSocket 是否启用")
    private Boolean webSocketEnabled;

    @Desc("WebSocket 路径")
    private String webSocketPath;

    @Desc("SSL是否启用")
    private Boolean sslEnabled;

    @Desc("SSL密钥文件密码")
    private String sslPassword;

    @Desc("心跳时间(秒), 默认60秒, 该值可被客户端连接时相应配置覆盖")
    private Integer keepAlive;

    @Desc("是否开启Epoll模式,linux上使用EpollEventLoopGroup会有较少的gc有更高级的特性，性能更好")
    private Boolean useEpoll;

    @Desc("Socket参数, 存放已完成三次握手请求的队列最大长度, 默认511长度")
    private Integer soBacklog;

    @Desc("Socket参数, 是否开启心跳保活机制, 默认开启")
    private Boolean soKeepAlive;

    @Desc("转发kafka主题")
    private String producerTopic;

    @Desc("MQTT.Connect消息必须通过用户名密码验证")
    private Boolean mqttPasswordMust;

    @Desc("是否启用kafka消息转发")
    private Boolean kafkaBrokerEnabled;

}
