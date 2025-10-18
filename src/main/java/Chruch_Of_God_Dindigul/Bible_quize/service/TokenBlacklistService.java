package Chruch_Of_God_Dindigul.Bible_quize.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final Cache tokenBlacklistCache;

    public TokenBlacklistService(CacheManager cacheManager) {
        this.tokenBlacklistCache = cacheManager.getCache("tokenBlacklist");
    }

    public void blacklistToken(String token) {
        // Add the token to the cache. The value doesn't matter.
        tokenBlacklistCache.put(token, true);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistCache.get(token) != null;
    }
}