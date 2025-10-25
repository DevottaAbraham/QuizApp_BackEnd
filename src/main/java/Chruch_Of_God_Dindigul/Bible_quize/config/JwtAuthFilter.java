package Chruch_Of_God_Dindigul.Bible_quize.config;

import Chruch_Of_God_Dindigul.Bible_quize.service.JwtService;
import Chruch_Of_God_Dindigul.Bible_quize.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RequestMatcher publicEndpoints = new OrRequestMatcher(
        new AntPathRequestMatcher("/api/auth/login"),
        new AntPathRequestMatcher("/api/auth/register"),
        new AntPathRequestMatcher("/api/auth/register-admin"),
        new AntPathRequestMatcher("/api/auth/setup-status"),
        new AntPathRequestMatcher("/api/auth/refresh"),
        new AntPathRequestMatcher("/api/auth/admin-forgot-password"), // Use the correct forgot password endpoint
        new AntPathRequestMatcher("/uploads/**"),
        new AntPathRequestMatcher("/error"), // Standard error pages
        new AntPathRequestMatcher("/api/content/home") // Public content
    );

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (publicEndpoints.matches(request) || "/error".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        final String username;

        // Extract JWT from the httpOnly cookie named "accessToken"
        if (request.getCookies() != null) {
            jwt = Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (jwt == null) {
            // CRITICAL SECURITY FIX: If the endpoint is protected and no JWT is present,
            // we must immediately reject the request.
            customAuthenticationEntryPoint.commence(request, response, new org.springframework.security.core.AuthenticationException("JWT token is missing.") {});
            return; // Stop the filter chain.
        }

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // CRITICAL FIX: Extract authorities directly from the token's claims
                    // This ensures the user's roles are correctly loaded for every request.
                    Claims claims = jwtService.extractAllClaims(jwt);
                    List<String> authoritiesList = claims.get("authorities", List.class);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials are not needed as we are using a token
                            jwtService.getAuthoritiesFromClaims(authoritiesList)
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If any exception occurs during token validation (e.g., token expired, malformed),
            // we must ensure the request is rejected. We delegate this to the entry point.
            customAuthenticationEntryPoint.commence(request, response, new org.springframework.security.core.AuthenticationException("Invalid JWT token: " + e.getMessage()) {});
            return; // Stop the filter chain
        }

        filterChain.doFilter(request, response);
    }
}