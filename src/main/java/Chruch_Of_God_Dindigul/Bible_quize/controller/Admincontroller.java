package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.UserDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.RegistrationRequest;
import Chruch_Of_God_Dindigul.Bible_quize.dto.MonthlyPerformanceDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.HomePageContent;
import Chruch_Of_God_Dindigul.Bible_quize.service.HomePageContentService;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.core.context.SecurityContextHolder;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/admin")
class Admincontroller {

    private static final Logger logger = LoggerFactory.getLogger(Admincontroller.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final QuestionService questionService;
    private final HomePageContentService homePageContentService;
    private final ScoreService scoreService;
    @Value("${application.security.admin-setup-token}") private String adminSetupToken;

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
     * Gets the details for a single user by their ID.
     * This is used by the admin frontend to get user details for display.
     *
     * @param userId The ID of the user to retrieve.
     * @return A DTO with the user's information.
     */
    @GetMapping(value = "/users/{userId}", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        logger.info("Admincontroller: Received GET request for user details with ID: {}", userId);
        return userService.findById(userId)
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getRole(), user.isMustChangePassword()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Admincontroller: User with ID {} not found.", userId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Gets the monthly performance data for a specific user.
     * This data is structured to be easily used by frontend charting libraries for animation.
     * @param userId The ID of the user.
     * @return A list of objects, each containing a month and the user's average score percentage for that month.
     */
    @GetMapping("/users/{userId}/performance")
    public ResponseEntity<List<MonthlyPerformanceDTO>> getUserPerformance(@PathVariable Long userId) { // Removed produces for now, as it's not the core issue
        logger.info("Admincontroller: Received GET request for user performance with ID: {}", userId);
        User user = userService.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Admincontroller: User with ID {} not found for performance request.", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });
        List<MonthlyPerformanceDTO> performanceData = scoreService.getMonthlyPerformanceForUser(user);
        logger.info("Admincontroller: Returning performance data for user ID: {}", userId);
        return ResponseEntity.ok(performanceData);
    }

    /**
     * Gets the overall monthly performance data across all users.
     * This is for the main chart on the admin "View Scores" page.
     */
    @GetMapping("/scores/monthly-performance")
    public ResponseEntity<List<MonthlyPerformanceDTO>> getMonthlyPerformance() {
        List<MonthlyPerformanceDTO> performanceData = scoreService.getMonthlyPerformance();
        return ResponseEntity.ok(performanceData);
    }

    /**
     * Creates a new user with the ADMIN role.
     * This provides a dedicated and secure endpoint for adding new administrators.
     */
   // d:\Quize Website Design\Quiz_App_BackEnd\Bible_quize\src\main\java\Chruch_Of_God_Dindigul\Bible_quize\controller\Admincontroller.java
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(@RequestBody RegistrationRequest registrationRequest) {
        // If the username already exists, inform the admin instead of creating a duplicate.
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }
        User user = User.builder()
                .username(registrationRequest.getUsername())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(Role.ADMIN) // Explicitly set role to ADMIN
                .build();

        User savedUser = userService.createUser(user);
        return new ResponseEntity<>(new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole(), savedUser.isMustChangePassword()), HttpStatus.CREATED);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody RegistrationRequest registrationRequest, Authentication authentication) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }
        User user = User.builder()
                .username(registrationRequest.getUsername())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userService.createUser(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole(), savedUser.isMustChangePassword());
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

        // Best Practice: Invalidate the user's refresh token before deleting them.
        // This helps prevent errors if the deleted user's browser tries to refresh their session.
        User user = userToDelete.get();
        user.setRefreshToken(null);
        userService.updateUser(user);

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

        // Best Practice: Invalidate the user's refresh token before deleting them.
        User user = userToDelete.get();
        user.setRefreshToken(null);
        userService.updateUser(user);

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
        // FINAL FIX: Explicitly set the mustChangePassword field during an update
        // to prevent it from ever being considered null by the persistence context.
        userToUpdate.setMustChangePassword(registrationRequest.isMustChangePassword());

        // 4. Save the updated user and return it.
        User savedUser = userService.updateUser(userToUpdate);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole(), savedUser.isMustChangePassword());
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
        
        // Allow admins to reset other admins' passwords, but not their own.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot reset your own password using this tool."));
        }

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // Force the user to change their password on next login
        user.setMustChangePassword(true);

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

    /**
     * Downloads a PDF report of all questions in the system.
     * @return A PDF file.
     */
    @GetMapping("/questions/download/pdf")
    public ResponseEntity<InputStreamResource> downloadQuestionsPdf() {
        List<QuestionDTO> questions = questionService.getAllQuestions();
        // Reusing the ScoreService for PDF generation logic
        ByteArrayInputStream bis = scoreService.generateQuestionsPdf(questions);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=questions-history.pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }

    /**
     * Downloads a plain text (.txt) report of all questions in Tamil.
     * @return A text file.
     */
    @GetMapping("/questions/download/txt")
    public ResponseEntity<InputStreamResource> downloadQuestionsTxt() {
        List<QuestionDTO> questions = questionService.getAllQuestions();
        ByteArrayInputStream bis = scoreService.generateQuestionsTxt(questions);

        HttpHeaders headers = new HttpHeaders();
        // Set the filename for the download
        headers.add("Content-Disposition", "attachment; filename=questions-tamil.txt");

        // Set content type to text/plain and specify UTF-8 encoding
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/plain; charset=utf-8")).body(new InputStreamResource(bis));
    }

    /**
     * Deletes all questions from the system. This is an irreversible action.
     * @return A success message.
     */
    @DeleteMapping("/questions/all")
    public ResponseEntity<?> deleteAllQuestions() {
        questionService.deleteAllQuestions();
        return ResponseEntity.ok(Map.of("message", "All questions have been successfully deleted."));
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