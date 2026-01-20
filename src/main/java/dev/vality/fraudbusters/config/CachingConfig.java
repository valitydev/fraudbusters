package dev.vality.fraudbusters.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CachingConfig {

    @Value("${cache.expire-after-access-seconds:100}")
    private int expireAfterAccessSeconds;
    @Value("${cache.inspect-user-expire-after-access-seconds:300}")
    private int inspectUserExpireAfterAccessSeconds;

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("resolveCountry", "isNewShop");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(500)
                .expireAfterAccess(expireAfterAccessSeconds, TimeUnit.SECONDS));
        return cacheManager;
    }

    @Bean(name = "inspectUserCacheManager")
    public CacheManager inspectUserCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("inspectUser");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterAccess(inspectUserExpireAfterAccessSeconds, TimeUnit.SECONDS));
        return cacheManager;
    }
}
