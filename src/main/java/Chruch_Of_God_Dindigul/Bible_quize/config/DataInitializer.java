package Chruch_Of_God_Dindigul.Bible_quize.config;

import java.util.Optional;
import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Find the admin user, if it exists.
        Optional<User> adminOptional = userService.findByUsername("admin");

        if (adminOptional.isEmpty()) {
            // If the admin user doesn't exist, create it.
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("adminpassword"));
            admin.setRole(Role.ADMIN);
            userService.createUser(admin);
            System.out.println("Created default admin user with username 'admin' and password 'adminpassword'");
        } else {
            // If the admin user exists, ensure its password is the correctly hashed version.
            User admin = adminOptional.get();
            if (!passwordEncoder.matches("adminpassword", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("adminpassword"));
                userService.createUser(admin); // .save() will update the existing user
                System.out.println("Updated default admin user's password to a secure hash.");
            }
        }
    }
}
