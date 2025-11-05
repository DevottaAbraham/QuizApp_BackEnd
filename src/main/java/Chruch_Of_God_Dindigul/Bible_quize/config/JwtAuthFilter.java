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
            // If there's no token, just continue the filter chain.
            // Spring Security will handle whether the endpoint is public or needs authentication.
            filterChain.doFilter(request, response);
            return; // Stop the filter chain.
        }

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // CRITICAL SECURITY FIX: Extract authorities from the token's claims.
                    // This ensures the user's roles (e.g., ROLE_ADMIN) are correctly loaded for every request.
                    Claims claims = jwtService.extractAllClaims(jwt);
                    // Ensure the list is not null before processing
                    List<String> authoritiesList = claims.get("authorities", List.class);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials are not needed as we are using a token
                            // CRITICAL FIX: Convert the list of strings from claims into GrantedAuthority objects.
                            // This was the missing step that caused all admin access to be denied.
                            authoritiesList == null ? List.of() : authoritiesList.stream()
                                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList())
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