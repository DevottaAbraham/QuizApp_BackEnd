package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * This configuration takes manual control of Flyway's initialization to resolve startup dependency issues in cloud environments.
 */
@Configuration
public class FlywayConfig {

    /**
     * This bean overrides Spring Boot's default Flyway initializer.
     * By using @DependsOn("entityManagerFactory"), we explicitly command Spring to ensure that
     * the entire JPA infrastructure (including the DataSource with the resolved URL) is fully
     * initialized BEFORE this bean is created and Flyway migration is attempted.
     */
    @Bean
    @DependsOn("entityManagerFactory")
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, (f) -> f.migrate());
    }
}