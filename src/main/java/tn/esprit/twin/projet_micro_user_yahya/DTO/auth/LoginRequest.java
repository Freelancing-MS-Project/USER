package tn.esprit.twin.projet_micro_user_yahya.DTO.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
