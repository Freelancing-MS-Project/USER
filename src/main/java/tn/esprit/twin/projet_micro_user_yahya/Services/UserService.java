package tn.esprit.twin.projet_micro_user_yahya.Services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        String keycloakId = keycloakService.createUser(request);

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setCreatedAt(LocalDateTime.now());

        return userRepo.save(user);
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
        userRepo.deleteById(id);
    }
}
