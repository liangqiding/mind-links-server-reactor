package com.mind.links.netty.mqtt.config;

import com.mind.links.netty.mqtt.mqttStore.cache.DestroyCache;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;


/**
 * description : TODO Redisson 核心配置
 *
 * @author : qiDing
 * date: 2020-12-10 14:27
 * @version v1.0.0
 */
@Configuration
@ComponentScan
@EnableCaching
@RequiredArgsConstructor
public class RedissonConfig {

    @Autowired
    private RedissonProperties redissonProperties;


    /**
     * 配置参考 https://github.com/redisson/redisson/wiki/2.-Configuration
     */
    @Bean
    public RedissonReactiveClient getRedisson() throws Exception {
        RedissonReactiveClient redisson = null;
        Config config = new Config();
        config.useSingleServer()
                .setPassword(redissonProperties.getPassword())
                .setDatabase(redissonProperties.getDatabase())
                .setAddress(redissonProperties.getAddress())
                .setConnectionPoolSize(redissonProperties.getConnectionPoolSize())
                .setTimeout(redissonProperties.getTimeout());
        Codec codec = (Codec) ClassUtils.forName(redissonProperties.getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codec);
        redisson = Redisson.createReactive(config);
        return redisson;
    }
}
