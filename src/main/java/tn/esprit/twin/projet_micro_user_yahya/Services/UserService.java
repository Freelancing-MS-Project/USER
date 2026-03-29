package tn.esprit.twin.projet_micro_user_yahya.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.FaceVerificationResponse;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Repositories.UserRepo;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor

public class UserService implements IUserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${face.id.service.url:http://localhost:5000/verify}")
    private String faceIdServiceUrl;


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

    @Override
    public ResponseEntity<FaceVerificationResponse> verifyUserFace(Long userId, MultipartFile file) {
        return verifyUserFace(userId, file, null);
    }

    @Override
    public Optional<FaceMatchResult> findBestFaceMatch(MultipartFile file, Double tolerance) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        List<User> usersWithImages = userRepo.findByUserImageIsNotNull();
        if (usersWithImages.isEmpty()) {
            return Optional.empty();
        }

        return usersWithImages.stream()
                .map(user -> mapFaceMatch(user, verifyUserFace(user.getId(), file, tolerance)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparingDouble(FaceMatchResult::confidence));
    }

    private ResponseEntity<FaceVerificationResponse> verifyUserFace(Long userId, MultipartFile file, Double tolerance) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload-image";
                }
            };

            HttpHeaders fileHeaders = new HttpHeaders();
            if (file.getContentType() != null && !file.getContentType().isBlank()) {
                fileHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
            } else {
                fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("userId", userId.toString());
            body.add("file", new HttpEntity<>(fileResource, fileHeaders));
            if (tolerance != null) {
                body.add("tolerance", tolerance.toString());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<FaceVerificationResponse> response = restTemplate.exchange(
                    faceIdServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    FaceVerificationResponse.class
            );

            FaceVerificationResponse responseBody = response.getBody();
            if (responseBody == null) {
                responseBody = new FaceVerificationResponse(null, null, "Empty response from Face ID service");
            }

            return ResponseEntity.status(response.getStatusCode()).body(responseBody);
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(parseFaceVerificationError(ex));
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Face ID service is unreachable",
                    ex
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Face ID service request failed",
                    ex
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded image", ex);
        }
    }

    private Optional<FaceMatchResult> mapFaceMatch(User user, ResponseEntity<FaceVerificationResponse> response) {
        FaceVerificationResponse body = response.getBody();
        HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());

        if (status.is2xxSuccessful() && body != null && Boolean.TRUE.equals(body.match()) && body.confidence() != null) {
            return Optional.of(new FaceMatchResult(user, body.confidence()));
        }

        if (status == HttpStatus.BAD_REQUEST && body != null && body.error() != null) {
            String error = body.error();
            if (error.contains("uploaded image") || error.contains("Uploaded file must be an image")) {
                throw new IllegalArgumentException(error);
            }
        }

        if (status == HttpStatus.BAD_GATEWAY) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Face ID service request failed");
        }

        return Optional.empty();
    }

    private FaceVerificationResponse parseFaceVerificationError(HttpStatusCodeException ex) {
        try {
            FaceVerificationResponse response = objectMapper.readValue(
                    ex.getResponseBodyAsByteArray(),
                    FaceVerificationResponse.class
            );
            if (response != null) {
                return response;
            }
        } catch (IOException ignored) {
        }

        String message = ex.getResponseBodyAsString();
        if (message == null || message.isBlank()) {
            message = "Face ID service request failed";
        }
        return new FaceVerificationResponse(null, null, message);
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
