package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import lombok.Data;

@Data
public class RegistrationRequest {
    private String username;
    private String password;
    private Role role;
}
