package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.UserDTO; // Keep one
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.RegistrationRequest;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin")
public class Admincontroller {

    private static final Logger logger = LoggerFactory.getLogger(Admincontroller.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final QuestionService questionService;

    @Autowired
    public Admincontroller(UserService userService, PasswordEncoder passwordEncoder, QuestionService questionService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.questionService = questionService;
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
        return ResponseEntity.ok(userService.findAllUsersAsDTO());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody RegistrationRequest registrationRequest, Authentication authentication) {
        if (userService.findByUsername(registrationRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username is already taken!"));
        }
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRole(registrationRequest.getRole());

        User savedUser = userService.createUser(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        Optional<User> userToDelete = userService.findById(id);

        if (userToDelete.isEmpty()) {
            return ResponseEntity.noContent().build(); // User already gone, idempotent success
        }

        // Security Check: Prevent the main 'admin' account from being deleted.
        // We check for the first user created, assuming that is the root admin.
        if (userToDelete.get().getId() == 1L) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "The root admin account cannot be deleted."));
        }

        userService.deleteUser(id);
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

        // Security Check: The main 'admin' user's role cannot be changed.
        if (userToUpdate.getId() == 1L && registrationRequest.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "The root admin's role cannot be changed."));
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
