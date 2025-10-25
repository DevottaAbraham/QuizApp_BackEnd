package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
    private boolean mustChangePassword; // Add this field

    // Add a constructor that includes the mustChangePassword field
    public UserDTO(Long id, String username, Role role, boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }
}