package Chruch_Of_God_Dindigul.Bible_quize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    // Use a standard ObjectMapper to safely write JSON.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // If the token is simply missing on an initial check, it's a normal flow, not a warning.
        // This is the expected behavior for any user who is not logged in.
        if (authException.getMessage().contains("JWT token is missing")) {
            logger.debug("Authentication check for '{}': No active session found (JWT token is missing). This is normal for unauthenticated users.", request.getRequestURI());
        } else {
            // For all other authentication errors (e.g., invalid signature, expired token, user not found),
            // it's an actual issue that should be logged as a warning.
            logger.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());
        }

        // Prevent writing to the response if it has already been committed.
        if (response.isCommitted()) {
            logger.error("Response has already been committed. Unable to send 401 error.");
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> data = new HashMap<>();
        data.put("error", "Unauthorized");
        data.put("message", "Authentication failed: " + authException.getMessage());
        data.put("path", request.getRequestURI());

        // Use the ObjectMapper to write the response. This is safer than response.getWriter().
        response.getOutputStream().println(objectMapper.writeValueAsString(data));
    }
}