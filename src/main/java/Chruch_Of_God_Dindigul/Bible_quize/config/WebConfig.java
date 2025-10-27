package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Apply to all your API endpoints
            .allowedOrigins("http://localhost:5173") // Your frontend's URL
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*") 
            .allowCredentials(true)
            .maxAge(3600L); // Optional: How long the pre-flight response can be cached (1 hour)
    }
}