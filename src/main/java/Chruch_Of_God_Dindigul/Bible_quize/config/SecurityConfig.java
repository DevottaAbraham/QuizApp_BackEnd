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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
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
        // Use MvcRequestMatcher for more precise endpoint matching
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // CRITICAL FIX: Explicitly configure Spring Security's CORS handling
                // to use the same settings defined in WebConfig.java. This ensures
                // that the security filter chain correctly applies our credential-allowing policy.
                .cors(Customizer.withDefaults())

                // Disable CSRF, as we'll use stateless authentication (JWT)
                .csrf(csrf -> csrf.disable())
                   .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    // --- PUBLIC ENDPOINTS (No Authentication Required) ---
                    auth
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**")).permitAll() // Allow all CORS pre-flight
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/login")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/register")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/register-admin")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/setup-status")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/refresh")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/auth/admin-forgot-password")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/content/**")).permitAll() // Allow public content
                        .requestMatchers(mvcMatcherBuilder.pattern("/uploads/**")).permitAll() // Allow access to uploaded files
                        .requestMatchers(mvcMatcherBuilder.pattern("/error")).permitAll()

                        // --- SPA Frontend Routes & Assets ---
                        .requestMatchers(mvcMatcherBuilder.pattern("/")).permitAll()
                        // Permit access to all static assets (HTML, JS, CSS, images, etc.).
                        .requestMatchers(mvcMatcherBuilder.pattern("/**/*.{js,css,html,png,jpg,jpeg,gif,svg,ico}")).permitAll()
                        // Allow all non-API, non-static file requests to be forwarded to the SPA.
                        // This regex matches paths that do not contain a dot, preventing it from matching file requests (e.g., .css, .js).
                        // .requestMatchers(mvcMatcherBuilder.pattern("/**/{path:[^\\.]*}")).permitAll()

                        // --- ADMIN-ONLY ENDPOINTS ---
                        // CRITICAL FIX: Use hasRole("ADMIN"). This correctly checks for the "ROLE_ADMIN" authority
                        // that is being set in the JWT, fixing the access denied error for all admin endpoints.
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasRole("ADMIN")

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