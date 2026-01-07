package com.hmdp.utils;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BloomFilterInitializer {

    @Resource
    private RBloomFilter<Long> shopIdBloomFilter;
    @Resource
    private IShopService shopService;

    @PostConstruct
    public void init(){
        if (shopIdBloomFilter.count() > 0) {
            log.info("布隆过滤器已存在数据，元素数量: {}", shopIdBloomFilter.count());
            return; // 已有数据，跳过初始化
        }
        List<Long> shopIds = shopService.query()
                .select("id")
                .list().stream().map(Shop::getId)
                .collect(Collectors.toList());
        for (Long shopId : shopIds) {
            shopIdBloomFilter.add(shopId);
        }
        log.info("预加载 {} 个店铺ID到布隆过滤器", shopIds.size());
    }
}
