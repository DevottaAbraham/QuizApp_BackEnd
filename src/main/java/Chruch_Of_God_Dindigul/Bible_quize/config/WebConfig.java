package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // This forwards requests for SPA routes to the index.html page.
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/**/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/setup").setViewName("forward:/index.html");
    }
}
