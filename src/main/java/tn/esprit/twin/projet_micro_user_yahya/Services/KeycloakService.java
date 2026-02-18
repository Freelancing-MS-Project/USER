package tn.esprit.twin.projet_micro_user_yahya.Services;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.Role;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(UserRequest request) {

        UsersResource users = keycloak.realm(realm).users();

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(true);

        Response response = users.create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak");
        }

        String location = response.getHeaderString("Location");
        String userId = location.substring(location.lastIndexOf('/') + 1);

        // IMPORTANT : récupérer le userResource APRÈS création
        UserResource userResource = keycloak.realm(realm).users().get(userId);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());

        userResource.resetPassword(credential);

        // Assigner rôle
        String roleName = mapToKeycloakRole(request.getRole());
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        userResource.roles().realmLevel().add(List.of(role));

        System.out.println("User created with password and role: " + userId);

        return userId;
    }


    public void deleteUser(String keycloakId) {
        keycloak.realm(realm).users().get(keycloakId).remove();
    }

    private void setPassword(String userId, String rawPassword) {

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(rawPassword);

        keycloak
                .realm(realm)
                .users()
                .get(userId)
                .resetPassword(credential);

        System.out.println("Password successfully set for user: " + userId);
    }


    private void assignRealmRole(String userId, String roleName) {
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        List<RoleRepresentation> roles = Collections.singletonList(role);
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(roles);
    }

    // Mapping si ton enum est Client/Freelancer/Admin
    private String mapToKeycloakRole(Role role) {
        if (role == null) return "CLIENT";
        return switch (role) {
            case Admin -> "ADMIN";
            case Freelancer -> "FREELANCER";
            case Client -> "CLIENT";
        };
    }
}
