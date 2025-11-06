package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.LoginRequest;
import Chruch_Of_God_Dindigul.Bible_quize.dto.LoginResponse;
import Chruch_Of_God_Dindigul.Bible_quize.dto.RegistrationRequest;
import Chruch_Of_God_Dindigul.Bible_quize.dto.UserDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.JwtService;
import Chruch_Of_God_Dindigul.Bible_quize.service.TokenBlacklistService;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.Cookie;
import org.springframework.security.core.GrantedAuthority;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders; // Correct import for Spring's HttpHeaders
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional; 

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${application.security.admin-setup-token}")
    private String adminSetupToken;

    @Value("${application.security.cookie.secure:true}") // Default to true for production
    private boolean useSecureCookies;

    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, TokenBlacklistService tokenBlacklistService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        try {
            // Use the AuthenticationManager to validate the credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            // If authentication is successful, generate a token for the authenticated user.
            // The principal is our own User object because our UserService returns it.
            User user = (User) authentication.getPrincipal();

            // CRITICAL FIX: Add user roles to the JWT claims.
            Map<String, Object> claims = new HashMap<>();
            List<String> authorities = user.getAuthorities().stream()
                    .map(authority -> "ROLE_" + authority.getAuthority()) // Ensure ROLE_ prefix
                    .collect(Collectors.toList());
            claims.put("authorities", authorities);

            // Generate both access and refresh tokens
            String accessToken = jwtService.generateAccessToken(claims, user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Save the refresh token to the user in the database
            user.setRefreshToken(refreshToken);
            userService.updateUser(user);

            // Set tokens in secure, httpOnly cookies
            addTokenCookie(response, "accessToken", accessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
            addTokenCookie(response, "refreshToken", refreshToken, (int) (jwtService.getRefreshTokenExpiration() / 1000));

            logger.info("User '{}' logged in successfully with role {}", user.getUsername(), user.getRole());

            // The frontend will receive this response. It MUST check the 'role' field
            // to decide whether to redirect to the admin dashboard or the user page.
            LoginResponse loginResponse = new LoginResponse(
                user.getId(), 
                user.getUsername(), 
                user.getRole(), 
                user.isMustChangePassword() // Pass the flag to the frontend
            );
            return ResponseEntity.ok(loginResponse);

        } catch (AuthenticationException e) {
            logger.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            // If authentication fails, return a 401 Unauthorized response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest, HttpServletResponse response) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' is already taken.", registrationRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }

        // This endpoint should only register users with the USER role. Admin creation is handled by /register-admin.
        logger.info("Registering new user '{}' with role USER", registrationRequest.getUsername());

        User user = User.builder()
                .username(registrationRequest.getUsername())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userService.createUser(user);

        // --- Automatically log the user in after registration ---
        logger.info("User '{}' created successfully. Automatically logging in.", savedUser.getUsername());
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = savedUser.getAuthorities().stream()
                .map(authority -> "ROLE_" + authority.getAuthority()) // Ensure ROLE_ prefix
                .collect(Collectors.toList());
         claims.put("authorities", authorities);

        String accessToken = jwtService.generateAccessToken(claims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        savedUser.setRefreshToken(refreshToken);
        userService.updateUser(savedUser);

        // Set tokens in secure, httpOnly cookies
        addTokenCookie(response, "accessToken", accessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
        addTokenCookie(response, "refreshToken", refreshToken, (int) (jwtService.getRefreshTokenExpiration() / 1000));

        LoginResponse loginResponse = new LoginResponse(
            savedUser.getId(), 
            savedUser.getUsername(), 
            savedUser.getRole(), 
            savedUser.isMustChangePassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
    }

    /**
     * Handles the creation of an admin account during the initial setup phase.
     * This endpoint ensures that any user created through it is assigned the ADMIN role.
     * It is protected by checking if any admins already exist. If they do, this endpoint
     * will not allow the creation of another admin to prevent misuse after setup.
     */
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegistrationRequest registrationRequest, HttpServletResponse response) {
        // SECURITY: This endpoint should only function if no admins exist.
        if (userService.countUsersByRole(Role.ADMIN) > 0) { // <--- This is the check
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Setup is already complete. Cannot create new admin."));
        }

        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }

        logger.info("Registering new ADMIN user '{}' via setup page.", registrationRequest.getUsername());
        User user = User.builder()
                .username(registrationRequest.getUsername())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(Role.ADMIN) // Always create an ADMIN
                .build();
        User savedUser = userService.createUser(user);

        // Automatically log the new admin in
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = savedUser.getAuthorities().stream()
                .map(authority -> "ROLE_" + authority.getAuthority()).collect(Collectors.toList());
        claims.put("authorities", authorities);
        String accessToken = jwtService.generateAccessToken(claims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);
        savedUser.setRefreshToken(refreshToken);
        userService.updateUser(savedUser);
        addTokenCookie(response, "accessToken", accessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
        addTokenCookie(response, "refreshToken", refreshToken, (int) (jwtService.getRefreshTokenExpiration() / 1000));

        LoginResponse loginResponse = new LoginResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole(), savedUser.isMustChangePassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
        // ... rest of the code to create the first admin
    }

    /**
     * A secure endpoint for an admin to reset their own or another admin's password.
     * This is a public endpoint, but it's protected by requiring the admin-setup-token
     * in the request body, which is validated on the server.
     */
    @PostMapping("/admin-forgot-password")
    public ResponseEntity<?> adminForgotPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String providedToken = payload.get("adminSetupToken");

        // 1. Validate the provided token against the server's secret token.
        if (providedToken == null || !providedToken.equals(this.adminSetupToken)) {
            logger.warn("Admin forgot password attempt failed for user '{}' due to invalid token.", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid authorization token."));
        }

        // 2. Find the user and ensure they are an admin.
        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty() || userOptional.get().getRole() != Role.ADMIN) {
            logger.warn("Admin forgot password attempt for non-existent or non-admin user: {}", username);
            // Return a generic success message to prevent username enumeration.
            return ResponseEntity.ok(Map.of("message", "If a matching admin account is found, the password will be reset."));
        }

        User user = userOptional.get();

        // 3. Generate a new password and force a change on next login.
        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userService.updateUser(user);

        logger.info("Password for ADMIN user '{}' has been reset via the secure forgot-password endpoint.", username);

        return ResponseEntity.ok(Map.of("message", "Admin password has been reset.", "newPassword", newPassword));
    }

     @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> getSetupStatus() {
        // Correctly check if an ADMIN user exists to determine if setup is complete.
        boolean isSetupComplete = userService.countUsersByRole(Role.ADMIN) > 0;
        logger.info("Checking admin setup status. Is setup complete? {}", isSetupComplete);
        return ResponseEntity.ok(Map.of("isSetupComplete", isSetupComplete));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = null;
        String refreshToken = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (accessToken != null) {
            try { // Blacklist the access token
                tokenBlacklistService.blacklistToken(accessToken);
            } catch (Exception e) { /* Ignore if token is already invalid */ }
        }

        if (refreshToken != null) {
            try { // Invalidate the refresh token in the database
                final String finalRefreshToken = refreshToken; // Make refreshToken effectively final
                // CRITICAL FIX: Use a method that can extract the username even if the token is expired.
                // This ensures the user's refresh token can always be revoked on logout.
                String username = jwtService.extractUsernameIgnoringExpiration(finalRefreshToken);
                userService.findByUsername(username).ifPresent(user -> {
                    if (finalRefreshToken.equals(user.getRefreshToken())) {
                        user.setRefreshToken(null);
                        userService.updateUser(user);
                        logger.info("User '{}' refresh token revoked.", username);
                    }
                });
            } catch (Exception e) { /* Ignore if token is already invalid */ }
        }

        // Clear the cookies on the client side
        addTokenCookie(response, "accessToken", "", 0);
        addTokenCookie(response, "refreshToken", "", 0);

        logger.info("User successfully logged out.");
        return ResponseEntity.ok(Map.of("message", "You have been successfully logged out."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            // This is a normal and expected case when a user is not logged in. Log as INFO, not WARN.
            logger.debug("Refresh token not found in cookies during refresh attempt. This is normal for unauthenticated users.");
            // Return an empty 401 response. The frontend should interpret this as "user is not logged in"
            // and not display an error message.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = null; // Declare username outside the try block for the catch block
        try {
            username = jwtService.extractUsername(refreshToken);
            logger.info("Attempting to refresh token for user: {}", username);
            User user = (User) userService.loadUserByUsername(username);

            // 1. Check if the token from the cookie matches the one in the database
            if (!refreshToken.equals(user.getRefreshToken())) {
                logger.warn("Refresh token mismatch for user {}. Token in cookie was present but did not match database.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid refresh token."));
            }

            // 2. Check if the token itself is expired
            if (jwtService.isTokenExpired(refreshToken)) {
                logger.warn("Refresh token for user {} is expired.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh token expired."));
            }

            // All checks passed, generate a new access token
            Map<String, Object> claims = Map.of("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
            String newAccessToken = jwtService.generateAccessToken(claims, user);
            logger.info("Generated new access token during refresh: {}", newAccessToken);
            addTokenCookie(response, "accessToken", newAccessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
            
            logger.info("Token refreshed successfully for user '{}'.", username);
            return ResponseEntity.ok(Map.of("message", "Token refreshed successfully."));

        } catch (Exception e) {
            // This will catch errors from extractUsername or loadUserByUsername
            logger.error("Error during refresh token process for user: {}. Error: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired refresh token."));
        }
    }

    /**
     * A private helper method to create and configure a secure, httpOnly cookie.
     *
     * @param response The HttpServletResponse to add the cookie to.
     * @param name     The name of the cookie (e.g., "accessToken").
     * @param value    The value of the cookie (the token).
     * @param maxAge   The maximum age of the cookie in seconds.
     */
    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // CRITICAL FIX for Cross-Site Authentication:
        // When frontend and backend are on different domains (e.g., on Render),
        // browsers require `SameSite=None` and `Secure=true` for cookies to be sent.
        // We build the cookie header manually for full control.
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly", name, value, maxAge));
        if (useSecureCookies) {
            cookieHeader.append("; Secure; SameSite=None");
        }
        // Use org.springframework.http.HttpHeaders.SET_COOKIE
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHeader.toString());
    }

    /**
     * A protected endpoint to get the details of the currently authenticated user,
     * regardless of their role. This is used by the frontend to restore a session.
     * @param authentication The authentication object provided by Spring Security.
     * @return A DTO with the current user's information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        // Pass the mustChangePassword flag to the frontend
        UserDTO userDTO = new UserDTO(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), currentUser.isMustChangePassword());
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/force-change-password")
    public ResponseEntity<?> forceChangePassword(@RequestBody Map<String, String> payload, Authentication authentication) {
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "New passwords do not match or are empty."));
        }

        User currentUser = (User) authentication.getPrincipal();

        // Check if the old password (the temporary one) is correct
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "The temporary password is not correct."));
        }

        // Check if the user is actually required to change their password
        if (!currentUser.isMustChangePassword()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You are not required to change your password."));
        }

        // Update the password and reset the flag
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        currentUser.setMustChangePassword(false);
        userService.updateUser(currentUser);

        logger.info("User '{}' successfully changed their password after a forced reset.", currentUser.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully. You can now log in with your new password."));
    }
}
