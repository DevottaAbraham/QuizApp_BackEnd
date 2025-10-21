package Chruch_Of_God_Dindigul.Bible_quize.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // Use MvcRequestMatcher for more precise endpoint matching
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // Enable CORS and use the default configuration provided by WebConfig
                .cors(Customizer.withDefaults())
                // Disable CSRF, as we'll use stateless authentication (JWT)
                .csrf(csrf -> csrf.disable())
                // Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Permit all OPTIONS requests for CORS preflight
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**")).permitAll()
                        // Admin-only endpoints: Grant full access to all /api/admin/** routes.
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasRole("ADMIN")
                        // User-accessible endpoints (also accessible by Admin).
                        // This includes fetching user data, quiz history, notices, etc.
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/user/**"), mvcMatcherBuilder.pattern("/api/scores/**")).authenticated()
                        // Publicly accessible endpoints
                        .requestMatchers(
                                mvcMatcherBuilder.pattern("/api/auth/login"),
                                mvcMatcherBuilder.pattern("/api/auth/register"),
                                mvcMatcherBuilder.pattern("/api/auth/register-admin"),
                                mvcMatcherBuilder.pattern("/api/auth/setup-status"),
                                mvcMatcherBuilder.pattern("/api/auth/refresh"), // Allow access to the refresh endpoint
                                mvcMatcherBuilder.pattern("/api/auth/forgot-password-generate-temp"),
                                mvcMatcherBuilder.pattern("/api/auth/me")).permitAll() // Allow access to check session status
                        .requestMatchers(mvcMatcherBuilder.pattern("/uploads/**")).permitAll() // Allow public access to uploaded files
                        .requestMatchers(mvcMatcherBuilder.pattern("/error")).permitAll() // Allow access to the default error page
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/content/**")).permitAll() // Allow public access to content like the home page
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/quizzes/active")).permitAll()
                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}