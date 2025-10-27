package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.dto.UserDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService { // Corrected filename to UserService.java

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) { // Using constructor injection
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Creates a new user and ensures the transaction is committed immediately.
     * This prevents race conditions where a user is logged in before their record is visible
     * to subsequent requests.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User updateUser(User user) {
        // This method is functionally the same as createUser but provides better semantic clarity.
        return userRepository.save(user);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<UserDTO> findAllUsersAsDTO() {
        return userRepository.findAll().stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getRole(), user.isMustChangePassword()))
                .collect(Collectors.toList());
    }


    public List<UserDTO> findActiveUsers() {
        return userRepository.findActiveUsers().stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getRole(), user.isMustChangePassword()))
                .collect(Collectors.toList());
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public long countAllUsers() {
        return userRepository.count();
    }
}
