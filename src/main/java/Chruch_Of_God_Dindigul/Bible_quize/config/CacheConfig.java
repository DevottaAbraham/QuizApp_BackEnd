package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Configure a simple in-memory cache manager for the token blacklist.
        return new ConcurrentMapCacheManager("tokenBlacklist");
    }
}