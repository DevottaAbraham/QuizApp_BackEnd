package Chruch_Of_God_Dindigul.Bible_quize.dto;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;

public record LoginResponse(
    Long id,
    String username,
    Role role,
    boolean mustChangePassword
) {}