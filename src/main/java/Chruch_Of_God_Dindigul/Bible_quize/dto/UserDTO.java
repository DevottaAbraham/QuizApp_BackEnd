package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Role role;

    // Note: The password is intentionally excluded for security.
}