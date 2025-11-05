package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                // Allow both your local frontend and deployed frontend
                .allowedOrigins(
                        "http://localhost:5173",
                        "https://quizapp-1v3h.onrender.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // CRITICAL FIX: This tells the browser that the server allows cookies
                // to be sent with cross-origin requests. This is the key to solving
                // the "JWT token is missing" error in a deployed environment.
                .allowCredentials(true)
                .maxAge(3600L);
    }
}
