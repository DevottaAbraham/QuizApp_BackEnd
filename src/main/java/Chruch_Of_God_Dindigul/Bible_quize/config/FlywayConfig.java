package Chruch_Of_God_Dindigul.Bible_quize.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    /**
     * This bean manually triggers Flyway migration after the JPA EntityManagerFactory has been initialized.
     * By using CommandLineRunner and @DependsOn, we ensure that the DataSource is fully configured
     * with the environment variables before Flyway attempts to use it. This resolves the
     * "URL must start with 'jdbc'" error during startup in cloud environments like Render.
     */
    @Bean
    @DependsOn("entityManagerFactory")
    public CommandLineRunner flywayMigrate(DataSource dataSource) {
        return args -> {
            Flyway.configure().dataSource(dataSource).load().migrate();
        };
    }
}