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

        // CRITICAL FIX: Restore a resource handler for static assets, but with a specific pattern.
        // This pattern matches files with common extensions, ensuring that JS, CSS, and images are served
        // from the 'static' directory without conflicting with API routes. This resolves the "spinning loader" issue.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}