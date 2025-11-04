package Chruch_Of_God_Dindigul.Bible_quize.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // Inject the DATABASE_URL from the environment
    @Value("${DATABASE_URL}")
    private String databaseUrl;

    /**
     * Manually configures the DataSource bean. This approach resolves startup race conditions
     * in cloud environments like Render by ensuring the database URL is correctly processed.
     * It converts the postgres:// format provided by Render into the jdbc:postgresql:// format
     * required by the JDBC driver.
     */
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        // Convert the Render URL format (postgres://user:pass@host:port/db)
        // to the standard JDBC format (jdbc:postgresql://host:port/db?user=user&password=pass)
        String jdbcUrl = "jdbc:" + databaseUrl;
        dataSource.setJdbcUrl(jdbcUrl);
        return dataSource;
    }
}