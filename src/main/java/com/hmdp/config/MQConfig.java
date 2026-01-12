package com.hmdp.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    // 1. 定义队列名称
    public static final String SECKILL_QUEUE = "seckill.queue";

    // 2. 声明队列 (程序启动时自动创建)
    @Bean
    public Queue seckillQueue() {
        // durable: true 表示持久化，重启MQ队列还在
        return new Queue(SECKILL_QUEUE, true);
    }

    // 3. 配置 JSON 序列化器 (关键！没有这个你在网页看到的是乱码)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}