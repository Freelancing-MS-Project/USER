package tn.esprit.twin.projet_micro_user_yahya.DTO.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private String email;
    private String role;
}
