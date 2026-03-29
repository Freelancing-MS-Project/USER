package tn.esprit.twin.projet_micro_user_yahya.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Repositories.UserRepo;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class UserService implements IUserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;


    @Override
    public User register(UserRequest request) {
        return register(request, null);
    }

    @Override
    public User register(UserRequest request, MultipartFile file) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCin(request.getCin());
        user.setRole(request.getRole());
        attachProfileImage(user, file);
        user.setCreatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }


    @Override
    public User getCurrentUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User updateCurrentUser(String email, UserUpdateRequest request) {

        User user = getCurrentUser(email);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCin(request.getCin());
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
        userRepo.delete(user);
    }


    @Override
    public User createUser(User user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

    private void attachProfileImage(User user, MultipartFile file) {
        if (file == null) {
            return;
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        try {
            byte[] imageBytes = file.getBytes();
            if (ImageIO.read(new ByteArrayInputStream(imageBytes)) == null) {
                throw new IllegalArgumentException("Uploaded file is not a valid image");
            }

            user.setUserImage(imageBytes);
            user.setUserImageContentType(contentType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded image", e);
        }
    }

}
