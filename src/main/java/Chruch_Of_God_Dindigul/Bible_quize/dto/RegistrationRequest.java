package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import lombok.Data;

@Data
public class RegistrationRequest {
    private String username;
    private String password;
    private Role role;
    // This field must match the JSON key sent from the frontend ('adminSetupToken')
    private String adminSetupToken;

    // This field ensures that new users are not forced to change their password by default.
    private boolean mustChangePassword = false;
}
