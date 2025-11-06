package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
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

    // Forward all non-API, non-static-file paths to index.html for client-side routing.
    // This ensures that client-side routing works correctly when the frontend is served from the backend.
    // The /** pattern should be sufficient to catch all paths not handled by other controllers or resource handlers.
    // The order of these view controllers matters; more specific paths should come before more general ones.


   @Override
public void addViewControllers(ViewControllerRegistry registry) {
    // Forward all paths that do not contain a dot (i.e., are not files) to index.html.
    // This is the standard way to support client-side routing in a Spring Boot + SPA setup.
    registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
    registry.addViewController("/**/{path:[^\\.]*}").setViewName("forward:/index.html");
    // Also forward the root path to index.html
    registry.addViewController("/").setViewName("forward:/index.html");
}
}
