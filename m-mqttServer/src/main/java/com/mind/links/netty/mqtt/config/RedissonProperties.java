package com.mind.links.netty.mqtt.config;

import annotation.Desc;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * description : redisson配置
 * @author : qiDing
 * date: 2020-12-14 09:27
 * @version v1.0.0
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@ConfigurationProperties(prefix = "redisson.single")
@Component
@Desc("redisson配置")
public class RedissonProperties {

    @Desc("连接空闲超时，单位：毫秒")
    private Integer idleConnectionTimeout;

    @Desc("连接超时，单位：毫秒")
    private Integer connectTimeout;

    @Desc("命令等待超时，单位：毫秒")
    private Integer timeout;

    @Desc("命令失败重试次数")
    private Integer retryAttempts;

    @Desc("命令重试发送时间间隔，单位：毫秒")
    private Integer retryInterval;

    @Desc("密码")
    private String password;

    @Desc("单个连接最大订阅数量")
    private Integer subscriptionsPerConnection;

    @Desc("客户端名称")
    private String clientName;

    @Desc("服务器地址")
    private String address;

    @Desc("从节点发布和订阅连接的最小空闲连接数")
    private Integer subscriptionConnectionMinimumIdleSize;

    @Desc("从节点发布和订阅连接池大小")
    private Integer subscriptionConnectionPoolSize;

    @Desc("从节点最小空闲连接数")
    private Integer connectionMinimumIdleSize;

    @Desc("连接池大小")
    private Integer connectionPoolSize;

    @Desc("数据库序号")
    private Integer database;

    @Desc("DNS监测时间间隔，单位：毫秒")
    private Integer dnsMonitoringInterval;

    @Desc("线程池数量")
    private Integer threads;

    @Desc("Netty线程池数量")
    private Integer nettyThreads;

    @Desc("序列化方式")
    private String codec;

    @Desc("传输模式")
    private String transportMode;

}
