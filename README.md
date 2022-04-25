<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">mind-mqtt-reactor</h1>
<h4 align="center">基于Reactor3 netty的大型响应式分布式物联网智能家电系统,十万长连接</h4>
<h5  style="color: red" align="center" >本项目尚未完成，且已于2020.10.10停止维护更新</h5>
<p align="center">
	<a href="#"><img src="https://img.shields.io/badge/Springboot-2.5.3-blue"></a>
	<a href="#"><img src="https://img.shields.io/badge/license%20-MIT-green"></a>
	<a href="https://gitee.com/liangqiding/mind-links"><img src="https://img.shields.io/badge/%E7%A0%81%E4%BA%91-%E5%9B%BD%E5%86%85%E5%9C%B0%E5%9D%80-yellow"></a>
</p>



## 前言

`本项目尚未完成，且于2020-10-10停止更新`

`mind-links` 本项目基于netty的大型响应式分布式物联网智能家电系统，主要技术包括：mqtt,Reactor3,netty,SpringCloud,nacos,Elasticsearch,Kafka,docker,Redisson,mysql,mongodb,EKL

> 阅读本项目需要一定的Reactor及函数式编程基础
> 项目包含前端web页面,后端web服务,tcp服务器
> 整合mqtt协议支持，百万长连接设计方案

##### 已达成12万长连接后面有测试图

# mind-links-server 正文

#### 效果预览
> 工具 mqttx
![mqttx测试效果](https://gitee.com/liangqiding/mind-links-static/raw/master/server/mqttx.png)
> 工具 mqtt.fx
![mqtt.fx测试效果](https://gitee.com/liangqiding/mind-links-static/raw/master/server/mqttfx.png)

#### 压测效果

> 测试工具 jmeter
![测试工具](https://gitee.com/liangqiding/mind-links-static/raw/master/server/tool.png)

> 压测效果 单机下压测效果 稳定6万连接（自身配置限制 i5 6300hq+16G）
![测试工具](https://gitee.com/liangqiding/mind-links-static/raw/master/server/mqtt-test2.png)

> 压测效果 集群模式下（两节点）压测效果 达成12万连接
![测试工具](https://gitee.com/liangqiding/mind-links-static/raw/master/server/jiqun2tai.png)

>说明：由于测试机的端口限制,单机最多也就65553个端口了，所以理论上jmeter最大也就可以模拟6万个连接（系统本身也会有很多服务占用端口）

>使用虚拟机理论上是可以让测试机端口无限的，前提性能跟得上。但实际中我们发现，单台测试机跑jmeter到3万个连接，其实已经是极限了（内存和cpu性能问题）

>这里借来了多台测试机运行jmeter,两个性能较好的用来集群部署ming-links-server服务

>到了9万连接的时候，会明显感觉到连接变慢（这里感觉应该快到瓶颈了），而且偶尔会有连不上的问题，不过好在之前的连接都非常稳定，新的连接虽然连接变慢，但还是可以慢慢的增长，
>为了让其慢慢涨，编写了一个脚本程序，让测试机
>每隔一段时间连200个，最后连接稳定在12万多。

>后续会测试消息的并发量，及kafka的吞吐速度。

#### 1 mqtt功能介绍

| 功能                  | 说明                |   
|-----------------------|----------------------|
| 消息质量（QoS）        |   MQTT消息质量有三个等级，QoS 0，QoS 1和 QoS 2     |   
| 遗愿标志（Will Flag）  | 当遇到异常或客户端心跳超时的情况，MQTT服务器会替客户端发布一个Will消息   |  
| 异步发布/订阅实现      | 发布者和订阅者之间并不需要直接建立联系。    |
| MQTT的消息类型         | 完成mqtt的14种报文结构的功能实现   |
| 安全认证               | 应用层：MQTT支持客户标识、用户名和密码认传输层：传输层使用TLS，加密通讯   |
| 二进制格式            | MQTT基于二进制实现而不是字符串，MQTT固定报文头仅有两字节，所以相比其他协议，发送一条消息最省流量|
| JSON                  |易于机器解析和生成，有效提升网络传输效率   |
| basic64              |支持basic64转换传输  |


#### 2 组织结构
| 模块                  | 说明                      |   
|-----------------------|----------------------     |
| m-common              |   工具类及通用代码         |   
| m-links-mqttServer    |   mqtt服务器              |  
| m-links-mqttServer    |   socket，webSocket服务器 | 

##### 2.1 m-links-mqttServer
``` lua
m-links-mqttServer
├── common -- 项目内部工具类
├── config -- 核心配置如reactor线程池，redisson配置，通用项目配置
├── kafka -- kafka 相关
├── mqttFilter -- mqtt连接拦截器
├── mqttHandler -- mqtt报文处理
├── mqttInitializer -- netty服务器初始化工具
├── mqttServer -- netty服务器入口
└── mqttStore -- 消息Io操作，如缓存，转换等
```
##### 2.2 m-links-socketServer
``` lua
m-links-socketServer
├── common -- 项目内部工具类
├── config -- 核心配置如reactor线程池，redisson配置，通用项目配置
├── kafka -- kafka 相关
├── messageHandler -- 报文处理
├── nettyHandler -- 管道事件处理
├── nettyInitializer -- netty服务器初始化工具
└── mqttServer -- netty服务器入口
```
##### 2.3 m-common

``` lua
m-common
├── annotation -- 自定义注解
├── context -- spring上下文
├── entity -- 各种实体
├── exception -- 通用异常
├── response -- 通用响应
└── utils -- 通用工具类
```

# 3 核心配置

#### 业务流程

![项目架构](https://gitee.com/liangqiding/mind-links-static/raw/master/server/lc.png)

#### maven包
> 用到的核心pom包  | [版本号参考](./pom.xml)

```xml
        <dependency>
        		<groupId>org.redisson</groupId>
        		<artifactId>redisson</artifactId>
        </dependency>
         <dependency>
               <groupId>com.alibaba</groupId>
               	<artifactId>fastjson</artifactId>
         </dependency>
         <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-streams</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
        </dependency>
```

## 更新日志

**2020-10-10：** 

