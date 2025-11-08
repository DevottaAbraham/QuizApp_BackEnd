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

        // CRITICAL FIX: Explicitly add the resource handler for static assets.
        // Overriding addResourceHandlers disables Spring Boot's default static content handling.
        // This configuration re-enables it, allowing index.html, JS, and CSS files to be served,
        // which resolves the "v is not a function" and subsequent 401 errors.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}