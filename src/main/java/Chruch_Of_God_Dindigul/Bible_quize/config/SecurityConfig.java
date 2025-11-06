package Chruch_Of_God_Dindigul.Bible_quize.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

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
        // Use MvcRequestMatcher for more precise endpoint matching
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // Use the global CORS configuration defined in WebConfig.java
                .cors(Customizer.withDefaults())
                // Disable CSRF, as we'll use stateless authentication (JWT)
                .csrf(csrf -> csrf.disable())
                // Set session management to STATELESS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define authorization rules from most specific (public) to most general (authenticated)
                .authorizeHttpRequests(auth -> auth
                        // --- PUBLIC ENDPOINTS (No Authentication Required) ---
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/content/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/uploads/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/error")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/")).permitAll()

                        // --- ADMIN-ONLY ENDPOINTS ---
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasAuthority("ROLE_ADMIN")

                        // --- CATCH-ALL: Any other request must be authenticated ---
                        .anyRequest().authenticated()
                )
                // Add the custom JWT filter before the standard Spring Security filters
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Configure custom exception handlers
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}