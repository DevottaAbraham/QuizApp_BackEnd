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
import org.slf4j.Logger;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.itextpdf.io.exceptions.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // CRITICAL FIX: Use a robust request matcher to identify all public paths.
    // This is more reliable than a simple path.startsWith() check.
    private final RequestMatcher publicEndpoints = new OrRequestMatcher(
            // --- Public API Endpoints ---
            // CRITICAL FIX: Align with SecurityConfig by specifying exact public auth endpoints.
            new AntPathRequestMatcher("/api/auth/login"),
            new AntPathRequestMatcher("/api/auth/register"),
            new AntPathRequestMatcher("/api/auth/register-admin"),
            new AntPathRequestMatcher("/api/auth/setup-status"),
            new AntPathRequestMatcher("/api/auth/refresh"),
            new AntPathRequestMatcher("/api/content/**"),
            // --- Static Resources & File Uploads ---
            new AntPathRequestMatcher("/uploads/**"),
            new AntPathRequestMatcher("/error"),
            // --- SPA Frontend Routes & Assets ---
            new AntPathRequestMatcher("/"), // Root path for index.html
            new AntPathRequestMatcher("/**/*.{js,css,html,png,jpg,jpeg,gif,svg,ico}"), // All static assets
            // CRITICAL FIX: Replace the complex regex with the same simple and effective pattern used in SecurityConfig.
            // This ensures that any non-file, non-API route (like /login, /setup, /dashboard) is correctly identified as public.
            new AntPathRequestMatcher("/**/{path:[^\\.]*}") // All non-API, non-file routes
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException, java.io.IOException {
        if (publicEndpoints.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // This filter is now only executed for protected endpoints, as defined in SecurityConfig.
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
            // Since this filter only runs on protected routes, if the token is null,
            // we just continue the chain. Spring Security's authorization rules will
            // then determine if the request is allowed (for a public endpoint) or
            // rejected (for a protected endpoint).
            filterChain.doFilter(request, response);
            return;
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
            // By clearing the context, we ensure the user is treated as unauthenticated.
            SecurityContextHolder.clearContext();
            logger.debug("Invalid JWT token encountered: {}. Delegating to authentication entry point.", e.getMessage(), e);
            customAuthenticationEntryPoint.commence(request, response, new org.springframework.security.core.AuthenticationException("Invalid JWT token", e) {});
            return; // IMPORTANT: Stop the filter chain here.
        }

        filterChain.doFilter(request, response);

    }
}