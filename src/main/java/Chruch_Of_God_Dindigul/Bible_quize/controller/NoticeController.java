package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.NoticeDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.NoticeService;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    @Autowired
    public NoticeController(NoticeService noticeService, UserService userService) {
        this.noticeService = noticeService;
        this.userService = userService;
    }

    // Endpoint for authenticated users to view their relevant notices, now under /api/user
    @GetMapping("/user/notices")
    public ResponseEntity<List<NoticeDTO>> getNoticesForCurrentUser(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(noticeService.getNoticesForUser(currentUser.getId()));
    }

    // Admin-only endpoint to view ALL notices
    @GetMapping("/admin/notices")
    public ResponseEntity<List<NoticeDTO>> getAllNoticesForAdmin() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    // Admin-only endpoint to create a notice
    @PostMapping("/admin/notices")
    public ResponseEntity<NoticeDTO> createNotice(
            @RequestPart("notice") NoticeDTO noticeDTO,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        // Pass the DTO, the image file, and the author to the service
        NoticeDTO createdNotice = noticeService.createNotice(noticeDTO, image, currentUser);

        return new ResponseEntity<>(createdNotice, HttpStatus.CREATED);
    }

    // Admin-only endpoint to delete a notice
    @DeleteMapping("/admin/notices/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}