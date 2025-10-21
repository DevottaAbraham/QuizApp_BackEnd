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
                    .map(GrantedAuthority::getAuthority)
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
            return ResponseEntity.ok(new LoginResponse(user.getId(), user.getUsername(), user.getRole(), null, null));

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

        // NEW LOGIC: The first user to ever register becomes the site administrator.
        // All subsequent users will be regular users.
        boolean isFirstUser = userService.countAllUsers() == 0;
        Role userRole = isFirstUser ? Role.ADMIN : Role.USER;

        logger.info("Registering new user '{}' with role {}", registrationRequest.getUsername(), userRole);

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRole(userRole);

        User savedUser = userService.createUser(user);

        // --- Automatically log the user in after registration ---
        logger.info("User '{}' created successfully. Automatically logging in.", savedUser.getUsername());
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = savedUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("authorities", authorities);

        String accessToken = jwtService.generateAccessToken(claims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        savedUser.setRefreshToken(refreshToken);
        userService.updateUser(savedUser);

        // Set tokens in secure, httpOnly cookies
        addTokenCookie(response, "accessToken", accessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
        addTokenCookie(response, "refreshToken", refreshToken, (int) (jwtService.getRefreshTokenExpiration() / 1000));

        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole(), null, null));
    }

    @PostMapping("/forgot-password-generate-temp")
    public ResponseEntity<?> forgotPasswordGenerateTemp(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required."));
        }

        Optional<User> userOptional = userService.findByUsername(username);
        if (userOptional.isEmpty()) {
            // For security, always return a generic message to avoid username enumeration
            logger.warn("Forgot password attempt for non-existent user: {}", username);
            return ResponseEntity.ok(Map.of("message", "If a matching account is found, a new password will be displayed."));
        }

        User user = userOptional.get();

        // SECURITY FIX: Prevent this public endpoint from being used on ADMIN accounts.
        if (user.getRole() == Role.ADMIN) {
            logger.warn("Attempted to use public forgot-password for ADMIN account '{}'. This is not allowed.", username);
            return ResponseEntity.ok(Map.of("message", "For security reasons, administrator passwords cannot be reset using this form. Please contact another administrator."));
        }

        String newPassword = UUID.randomUUID().toString().substring(0, 8); // Generate an 8-character random password
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);

        logger.info("Temporary password generated for user: {}", username);

        // WARNING: Returning the plain-text password directly is INSECURE for production.
        return ResponseEntity.ok(Map.of("message", "A new temporary password has been generated.", "newPassword", newPassword));
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
                String username = jwtService.extractUsername(finalRefreshToken);
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh token not found."));
        }

        try {
            String username = jwtService.extractUsername(refreshToken);
            User user = (User) userService.loadUserByUsername(username);

            if (refreshToken.equals(user.getRefreshToken()) && !jwtService.isTokenExpired(refreshToken)) {
                Map<String, Object> claims = Map.of("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
                String newAccessToken = jwtService.generateAccessToken(claims, user);
                addTokenCookie(response, "accessToken", newAccessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
                return ResponseEntity.ok(Map.of("message", "Token refreshed successfully."));
            }
        } catch (Exception e) {
            // Catches any JWT parsing errors or other issues
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired refresh token."));
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
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(useSecureCookies); // Ensures the cookie is sent only over HTTPS.
        cookie.setPath("/");    // Makes the cookie available for all paths in the domain.
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * A protected endpoint to get the details of the currently authenticated user,
     * regardless of their role. This is used by the frontend to restore a session.
     * @param authentication The authentication object provided by Spring Security.
     * @return A DTO with the current user's information.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentAuthenticatedUser(Authentication authentication) {
        // CRITICAL FIX: Check if the user is actually authenticated.
        // If not, return a 401 Unauthorized response instead of crashing.
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = (User) authentication.getPrincipal();
        UserDTO userDTO = new UserDTO(currentUser.getId(), currentUser.getUsername(), currentUser.getRole());
        return ResponseEntity.ok(userDTO);
    }

}
