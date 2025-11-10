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
                .addResourceLocations("file:" + uploadDir + "/"); // This handler is correct and should remain.
        // CRITICAL FIX: The "/**" resource handler has been removed. It was incorrectly
        // intercepting all API calls and causing 404 errors. Spring Boot's default
        // static content handling combined with the SpaController is the correct approach.
    }
}