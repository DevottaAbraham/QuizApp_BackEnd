package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.dto.NoticeDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Notice;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.repository.NoticeRepository; // Corrected path
import Chruch_Of_God_Dindigul.Bible_quize.repository.UserRepository; // Corrected path
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Autowired
    public NoticeService(NoticeRepository noticeRepository, FileStorageService fileStorageService, UserRepository userRepository) {
        this.noticeRepository = noticeRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    public NoticeDTO createNotice(NoticeDTO noticeDTO, MultipartFile image, User author) {
        Notice notice = new Notice();
        notice.setTitle(noticeDTO.getTitle());
        notice.setContent(noticeDTO.getContent());
        notice.setAuthor(author);
        notice.setGlobal(noticeDTO.isGlobal());

        // If an image is provided, store it and set the URL
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image);
            notice.setImageUrl(imageUrl);
        }

        // If the notice is not global and has target user IDs, associate them.
        if (!noticeDTO.isGlobal() && noticeDTO.getTargetUserIds() != null && !noticeDTO.getTargetUserIds().isEmpty()) {
            List<User> targetUsers = userRepository.findAllById(noticeDTO.getTargetUserIds());
            notice.setTargetUsers(new HashSet<>(targetUsers));
        } else {
            // If it's global, ensure the target users set is empty
            notice.getTargetUsers().clear();
        }

        Notice savedNotice = noticeRepository.save(notice);
        return convertToDto(savedNotice);
    }

    // Gets notices relevant for a specific user (global + targeted)
    public List<NoticeDTO> getNoticesForUser(Long userId) {
        return noticeRepository.findNoticesForUser(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Gets all notices, intended for the admin view
    public List<NoticeDTO> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void deleteNotice(Long id) {
        // Optional: Add check to ensure notice exists before deleting
        noticeRepository.deleteById(id);
    }

    private NoticeDTO convertToDto(Notice notice) {
        return new NoticeDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImageUrl(),
                notice.isGlobal(),
                notice.getTargetUsers().stream().map(User::getId).collect(Collectors.toList()),
                notice.getCreatedAt(),
                notice.getAuthor() != null ? notice.getAuthor().getUsername() : "System"
        );
    }
}