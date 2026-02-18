package tn.esprit.twin.projet_micro_user_yahya.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Repositories.UserRepo;

import java.time.LocalDateTime;
import java.util.List;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;

@Service
@RequiredArgsConstructor

public class UserService implements IUserService {

    private final UserRepo userRepo;
    private final KeycloakService keycloakService;


    @Override
    public User register(UserRequest request) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String keycloakId = null;

        try {
            // 1) Create in Keycloak
            keycloakId = keycloakService.createUser(request);

            // 2) Save local profile
            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setRole(request.getRole());
            user.setCreatedAt(LocalDateTime.now());

            return userRepo.save(user);

        } catch (Exception e) {

            // rollback Keycloak if DB fails or any error happens after user creation
            if (keycloakId != null) {
                try { keycloakService.deleteUser(keycloakId); } catch (Exception ignored) {}
            }

            throw new RuntimeException("Register failed: " + e.getMessage());
        }
    }


    @Override
    public User getCurrentUser(String keycloakId) {
        return userRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User updateCurrentUser(String keycloakId, UserUpdateRequest request) {

        User user = getCurrentUser(keycloakId);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Override
    public void deleteUser(Long id) {
        User user = getUserById(id);
        keycloakService.deleteUser(user.getKeycloakId());
        userRepo.delete(user);
    }


    @Override
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepo.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User updateUser(Long id, User updatedUser) {
        User user = getUserById(id);

        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setCin(updatedUser.getCin());
        user.setRole(updatedUser.getRole());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

}
