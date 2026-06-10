package com.recaring.config.infra;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("careRelationship",
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000).build());
        // GPS POST hot path: device token → wardKey (immutable after issuance)
        manager.registerCustomCache("deviceToken",
                Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(2000).build());
        return manager;
    }
}
