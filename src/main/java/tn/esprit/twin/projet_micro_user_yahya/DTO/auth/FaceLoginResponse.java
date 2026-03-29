package tn.esprit.twin.projet_micro_user_yahya.DTO.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FaceLoginResponse {
    private String token;
    private String tokenType;
    private String email;
    private String role;
    private Long userId;
    private Double confidence;
}
