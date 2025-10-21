package Chruch_Of_God_Dindigul.Bible_quize.service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import java.security.Key;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class JwtService {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSignKey() {
        // Use the raw bytes of the secret key string for HMAC-SHA algorithms.
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .claims().add(claims).and() // Correctly add the extra claims to the builder
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSignKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                // No extra claims are needed for the refresh token
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSignKey())
                .compact();
    }

    // ✅ Extract all claims (modern API)
    public Claims extractAllClaims(String token) { // This was likely private, making it public
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token) // Use parseSignedClaims for modern API
                .getPayload();
    }

    // ✅ Extract username from token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ✅ Check if token is expired
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            // If the token is malformed and we can't extract an expiration, treat it as expired/invalid.
            return true;
        }
    }

    // ✅ Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // A token is valid if the username matches, it's not expired, AND it's not blacklisted.
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !tokenBlacklistService.isTokenBlacklisted(token));
    }

    public List<GrantedAuthority> getAuthoritiesFromClaims(List<String> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
