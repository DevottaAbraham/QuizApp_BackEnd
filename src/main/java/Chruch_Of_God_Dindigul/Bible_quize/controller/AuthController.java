package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.LoginRequest;
import Chruch_Of_God_Dindigul.Bible_quize.dto.LoginResponse;
import Chruch_Of_God_Dindigul.Bible_quize.dto.RegistrationRequest;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.JwtService;
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
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from your frontend
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.admin-setup-token}")
    private String adminSetupToken;

    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
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

            // Generate the token with the claims.
            String token = jwtService.generateToken(claims, user);

            logger.info("User '{}' logged in successfully with role {}", user.getUsername(), user.getRole());

            // The frontend will receive this response. It MUST check the 'role' field
            // to decide whether to redirect to the admin dashboard or the user page.
            return ResponseEntity.ok(new LoginResponse(user.getId(), user.getUsername(), user.getRole(), token));

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
    public ResponseEntity<?> register(@RequestBody RegistrationRequest registrationRequest) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' is already taken.", registrationRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }

        // NEW LOGIC: The first user to ever register becomes the site administrator.
        // All subsequent users will be regular users.
        boolean isFirstUser = userService.countAllUsers() == 0;
        Role userRole = isFirstUser ? Role.ADMIN : Role.USER;

        // If this is the first user (admin), we must validate the setup token.
        if (isFirstUser) {
            if (adminSetupToken == null || adminSetupToken.isBlank() || !adminSetupToken.equals(registrationRequest.getAdminSetupToken())) {
                logger.warn("Admin setup failed: Invalid setup token provided by user '{}'", registrationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Invalid setup token."));
            }
        }
        logger.info("Registering new user '{}' with role {}", registrationRequest.getUsername(), userRole);

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRole(userRole);

        User savedUser = userService.createUser(user);

        String message = (userRole == Role.ADMIN)
                ? "Admin user registered successfully!"
                : "User registered successfully!";
        
        logger.info("User '{}' created successfully with ID {}", savedUser.getUsername(), savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", message));
    }

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> getSetupStatus() {
        boolean isSetupComplete = userService.countAllUsers() > 0;
        logger.info("Checking application setup status. Is setup complete? {}", isSetupComplete);
        return ResponseEntity.ok(Map.of("isSetupComplete", isSetupComplete));
    }
}
