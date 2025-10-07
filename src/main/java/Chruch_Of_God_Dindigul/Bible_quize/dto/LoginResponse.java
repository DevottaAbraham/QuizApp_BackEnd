package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String username;
    private Role role;
    private String token;
}