package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the 'uploads' directory to be accessible via '/uploads/**'
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");

        // CRITICAL FIX: The overly broad "/**" resource handler is removed.
        // Spring Boot's default behavior is to automatically serve content from "classpath:/static/".
        // Removing this explicit, greedy handler resolves the conflict where it was incorrectly
        // intercepting both API calls and SPA routes, causing 404 errors.
    }
}