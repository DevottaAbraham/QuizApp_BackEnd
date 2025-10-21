package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.UserDTO; // Keep one
import Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.RegistrationRequest;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import Chruch_Of_God_Dindigul.Bible_quize.service.HomePageContentService;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin")
class Admincontroller {

    private static final Logger logger = LoggerFactory.getLogger(Admincontroller.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final QuestionService questionService;
    private final HomePageContentService homePageContentService;
    private final ScoreService scoreService;

    @Autowired
    public Admincontroller(UserService userService, PasswordEncoder passwordEncoder, QuestionService questionService, HomePageContentService homePageContentService, ScoreService scoreService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.questionService = questionService;
        this.homePageContentService = homePageContentService;
        this.scoreService = scoreService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> getAdminDashboard() {
        // Debugging Step: Log the authorities of the current user.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("User '{}' attempting to access admin dashboard with authorities: {}", 
                    authentication.getName(), 
                    authentication.getAuthorities());

        return ResponseEntity.ok("Welcome to the Admin Dashboard!");
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalUsers = userService.countUsersByRole(Role.USER);
        long totalQuestions = questionService.getQuestionCount();
        long publishedQuestions = questionService.getPublishedQuestionCount();
        Map<String, Object> stats = Map.of(
            "totalUsers", totalUsers,
            "totalQuestions", totalQuestions,
            "publishedQuestions", publishedQuestions,
            "quizzesTaken", 0 // Placeholder for now
        );
        return ResponseEntity.ok(stats);
    }

    // --- User Management Endpoints ---

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        // Filter the list to only include users with the 'USER' role.
        List<UserDTO> users = userService.findAllUsersAsDTO().stream()
                .filter(user -> user.getRole() == Role.USER)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Gets a list of all users with the ADMIN role.
     * This provides a dedicated endpoint for the "Manage Admins" page.
     */
    @GetMapping("/admins")
    public ResponseEntity<List<UserDTO>> getAllAdmins() {
        List<UserDTO> admins = userService.findAllUsersAsDTO().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    /**
     * Gets a list of all users who are considered "active"
     * (i.e., they have a non-null refresh token).
     */
    @GetMapping("/users/active")
    public ResponseEntity<List<UserDTO>> getActiveUsers() {
        List<UserDTO> activeUsers = userService.findActiveUsers();
        return ResponseEntity.ok(activeUsers);
    }

    /**
     * Gets the score history for a specific user, identified by their ID.
     * This allows an admin to view the performance of a single user.
     */
    @GetMapping("/users/{userId}/scores")
    public ResponseEntity<List<QuizResultDTO>> getScoresForUser(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<QuizResultDTO> history = scoreService.getScoreHistoryForUser(user);
        return ResponseEntity.ok(history);
    }

    /**
     * Gets all scores from all users.
     * This is for the main admin "View Scores" page.
     */
    @GetMapping("/scores")
    public ResponseEntity<List<QuizResultDTO>> getAllScores() {
        List<QuizResultDTO> allScores = scoreService.getAllScores();
        return ResponseEntity.ok(allScores);
    }

    /**
     * Gets the leaderboard data.
     * This is now an admin-only endpoint.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard() {
        List<LeaderboardDTO> leaderboard = scoreService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Creates a new user with the ADMIN role.
     * This provides a dedicated and secure endpoint for adding new administrators.
     */
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody RegistrationRequest registrationRequest) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRole(Role.ADMIN); // Always create an ADMIN
        User savedUser = userService.createUser(user);
        return new ResponseEntity<>(new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole()), HttpStatus.CREATED);
    }
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody RegistrationRequest registrationRequest, Authentication authentication) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        // Enforce that this endpoint can only create users with the USER role.
        user.setRole(Role.USER);

        User savedUser = userService.createUser(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    /**
     * Deletes a user, including other administrators.
     * A crucial security check prevents an admin from deleting their own account.
     */
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        // Security Critical: Prevent an admin from deleting themselves.
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot delete your own account."));
        }

        Optional<User> userToDelete = userService.findById(id);
        if (userToDelete.isEmpty()) {
            return ResponseEntity.noContent().build(); // User already gone, idempotent success
        }

        // Proceed with deletion if the user exists and is not the current admin.
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        Optional<User> userToDelete = userService.findById(id);

        if (userToDelete.isEmpty()) {
            return ResponseEntity.noContent().build(); // User already gone, idempotent success
        }

        // Security Enhancement: Prevent any admin account from being deleted via this endpoint.
        if (userToDelete.get().getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin accounts cannot be deleted."));
        }

        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody RegistrationRequest registrationRequest, Authentication authentication) {
        // 1. Find the user to update by ID.
        Optional<User> userToUpdateOptional = userService.findById(id);
        if (userToUpdateOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User userToUpdate = userToUpdateOptional.get();

        // Security Enhancement: Prevent any admin account from being modified via this endpoint.
        if (userToUpdate.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin accounts cannot be modified."));
        }
        
        // 2. Check for username conflict.
        Optional<User> existingUserWithSameUsername = userService.findByUsername(registrationRequest.getUsername());
        if (existingUserWithSameUsername.isPresent() && !existingUserWithSameUsername.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken by another user!"));
        }

        // 3. Update user details.
        userToUpdate.setUsername(registrationRequest.getUsername());
        if (registrationRequest.getPassword() != null && !registrationRequest.getPassword().isEmpty()) {
            userToUpdate.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        }
        if (registrationRequest.getRole() != null) {
            userToUpdate.setRole(registrationRequest.getRole());
        }

        // 4. Save the updated user and return it.
        User savedUser = userService.updateUser(userToUpdate);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Resets a user's password to a new, randomly generated one.
     * Returns the new plain-text password to the admin.
     */
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        Optional<User> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOptional.get();

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Cannot reset password for an admin account."));
        }

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);

        logger.info("Password for user '{}' has been reset by an admin.", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully.", "newPassword", newPassword));
    }

    // --- Content Management Endpoints ---

    @PutMapping("/content/home")
    public ResponseEntity<HomePageContent> updateHomePageContent(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        HomePageContent updatedContent = homePageContentService.updateHomePageContent(content);
        return ResponseEntity.ok(updatedContent);
    }

    // --- Question Management Endpoints ---

    @PostMapping("/questions")
    public ResponseEntity<QuestionDTO> createQuestion(@RequestBody QuestionDTO questionDTO, Authentication authentication) {
        // The Authentication principal is injected by Spring Security.
        // The principal's name is the username of the logged-in admin.
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        QuestionDTO createdQuestion = questionService.createQuestion(questionDTO, currentUser);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDTO>> getAllQuestions() {
        List<QuestionDTO> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        return questionService.getQuestionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable Long id, @RequestBody QuestionDTO questionDTO) {
        try {
            QuestionDTO updatedQuestion = questionService.updateQuestion(id, questionDTO);
            return ResponseEntity.ok(updatedQuestion);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions/{id}/publish")
    public ResponseEntity<QuestionDTO> publishQuestion(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String releaseDate = payload.get("releaseDate");
        String disappearDate = payload.get("disappearDate");
        QuestionDTO publishedQuestion = questionService.publishQuestion(id, releaseDate, disappearDate);
        return ResponseEntity.ok(publishedQuestion);
    }

    @PostMapping("/questions/bulk/publish")
    public ResponseEntity<?> publishBulkQuestions(@RequestBody Map<String, Object> payload) {
        List<Integer> questionIdsInt = (List<Integer>) payload.get("questionIds");
        List<Long> questionIds = questionIdsInt.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        String releaseDate = (String) payload.get("releaseDate");
        String disappearDate = (String) payload.get("disappearDate");

        questionService.publishBulkQuestions(questionIds, releaseDate, disappearDate);
        
        return ResponseEntity.ok(Map.of("message", "Successfully published " + questionIds.size() + " questions."));
    }

    @DeleteMapping("/questions/published")
    public ResponseEntity<Void> deleteAllPublishedQuestions() {
        questionService.deleteAllPublishedQuestions();
        return ResponseEntity.noContent().build();
    }
}
