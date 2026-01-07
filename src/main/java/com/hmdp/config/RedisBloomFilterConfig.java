package com.hmdp.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisBloomFilterConfig {
    @Bean
    public RBloomFilter<Long> shopIdBloomFilter(RedissonClient redissonClient){
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("shop:bloom-filter");
        bloomFilter.tryInit(10000L,0.01);
        return bloomFilter;
    }
}
