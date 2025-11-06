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

    /**
     * CRITICAL FIX for Single-Page Application (SPA) routing.
     * This configuration ensures that when a user directly navigates to a frontend route
     * (e.g., /admin/setup, /login) or refreshes such a page, the Spring Boot server
     * forwards the request to the root `index.html`. This allows the React Router
     * to take over and render the correct component, preventing 404 errors.
     *
     * The pattern `/{path:[^\\.]*}` matches any path that does not contain a dot,
     * effectively ignoring requests for static assets like .js, .css, .ico, etc.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
