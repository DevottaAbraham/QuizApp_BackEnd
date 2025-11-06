package Chruch_Of_God_Dindigul.Bible_quize.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;    
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {        
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // Use MvcRequestMatcher for more precise, introspection-based endpoint matching.
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // Use the CORS configuration defined in WebConfig.java
                .cors(Customizer.withDefaults())
                // Disable CSRF as we are using stateless JWT authentication
                .csrf(csrf -> csrf.disable())
                // Set session management to STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define authorization rules from most specific to most general
                .authorizeHttpRequests(auth -> auth
                        // --- PUBLIC ENDPOINTS (No Authentication Required) ---
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**")).permitAll() // Allow all CORS pre-flight
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/**")).permitAll() // Allow all auth-related endpoints
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/content/**")).permitAll() // Allow public content
                        .requestMatchers(mvcMatcherBuilder.pattern("/uploads/**")).permitAll() // Allow access to uploaded files
                        .requestMatchers(mvcMatcherBuilder.pattern("/error")).permitAll() // Allow Spring's default error page
                        .requestMatchers(mvcMatcherBuilder.pattern("/")).permitAll() // Allow root access

                        // --- ADMIN-ONLY ENDPOINTS ---
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasAuthority("ROLE_ADMIN")

                        // --- AUTHENTICATED (ANY ROLE) ENDPOINTS ---
                        // Any other request that is not public or for admins must be authenticated.
                        .anyRequest().authenticated()
                )
                // Add the custom JWT filter before the standard authentication filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Configure custom exception handlers for authentication and access denied errors
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}