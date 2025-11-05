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
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final UserService userService; // Inject UserService directly

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService); // Use the injected UserService
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {        
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // Use MvcRequestMatcher for more precise endpoint matching
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // CRITICAL FIX: Explicitly configure Spring Security's CORS handling
                // to use the same settings defined in WebConfig.java. This ensures
                // that the security filter chain correctly applies our credential-allowing policy.
                .cors(Customizer.withDefaults())

                // Disable CSRF, as we'll use stateless authentication (JWT)
                .csrf(csrf -> csrf.disable())
                // Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider()) // Use the method to get the provider
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    // --- PUBLIC ENDPOINTS ---
                    // These endpoints are accessible to everyone, without authentication.
                    // This is the most critical section for fixing the 404/login issue.
                    auth
                        .requestMatchers(
                                mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**"), // Allow all CORS pre-flight requests
                                mvcMatcherBuilder.pattern("/api/auth/login"),
                                mvcMatcherBuilder.pattern("/api/auth/register"),
                                mvcMatcherBuilder.pattern("/api/auth/register-admin"),
                                mvcMatcherBuilder.pattern("/api/auth/setup-status"),
                                mvcMatcherBuilder.pattern("/api/auth/refresh"),
                                mvcMatcherBuilder.pattern("/api/auth/admin-forgot-password"),
                                mvcMatcherBuilder.pattern("/api/auth/logout"),
                                mvcMatcherBuilder.pattern("/api/content/home"),
                                mvcMatcherBuilder.pattern("/uploads/**"),
                                mvcMatcherBuilder.pattern("/error")
                        ).permitAll()

                        // --- ADMIN-ONLY ENDPOINTS ---
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasAuthority("ROLE_ADMIN")

                        // --- AUTHENTICATED (ANY ROLE) ENDPOINTS ---
                        // Any other request that is not public or for admins must be authenticated.
                        .anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}