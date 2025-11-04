package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class FlywayConfig {

    @Bean
    @DependsOn("entityManagerFactory")
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            // This customization ensures Flyway initializes after the JPA EntityManagerFactory.
        };
    }
}