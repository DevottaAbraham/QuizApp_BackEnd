package Chruch_Of_God_Dindigul.Bible_quize.service;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for blacklisting JWT tokens.
 * This is a simple in-memory blacklist. For a production environment,
 * a more robust solution like Redis should be used.
 */
@Service
public class TokenBlacklistService {

    // Using a synchronized set to ensure thread-safety for in-memory storage.
    private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
