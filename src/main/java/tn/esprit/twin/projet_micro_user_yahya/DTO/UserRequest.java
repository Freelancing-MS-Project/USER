package tn.esprit.twin.projet_micro_user_yahya.DTO;

import lombok.Data;
import tn.esprit.twin.projet_micro_user_yahya.Entities.Role;

@Data
public class UserRequest {

    private String email;
    private String firstName;
    private String lastName;
    private String cin;
    private Role role;
    private String password; // nécessaire pour Keycloak
}
