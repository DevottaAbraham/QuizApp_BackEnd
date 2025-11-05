package Chruch_Of_God_Dindigul.Bible_quize.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.security.jwt")
@Data
public class JwtProperties {

    /**
     * The Base64-encoded secret key for signing JWTs.
     */
    private String secretKey;

    /**
     * Access token validity in milliseconds. Defaults to 1 hour.
     */
    private long accessTokenExpiration = 3600000;

    /**
     * Refresh token validity in milliseconds. Defaults to 7 days.
     */
    private long refreshTokenExpiration = 604800000;
}