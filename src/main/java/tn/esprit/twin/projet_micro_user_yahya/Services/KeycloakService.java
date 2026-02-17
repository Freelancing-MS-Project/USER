package tn.esprit.twin.projet_micro_user_yahya.Services;

import org.springframework.stereotype.Service;

@Service
public class KeycloakService {

    public String createUser(Object request) {

        // TEMPORAIRE : on simule un ID Keycloak
        return java.util.UUID.randomUUID().toString();
    }
}
