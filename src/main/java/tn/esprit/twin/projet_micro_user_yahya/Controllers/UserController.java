package tn.esprit.twin.projet_micro_user_yahya.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.Role;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 🔐 ADMIN crée un user local uniquement (rarement utilisé)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User create(@RequestBody User user) {
        return userService.createUser(user);
    }

    // 🔐 ADMIN voit tous les users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAll() {
        return userService.getAllUsers();
    }

    // 🔐 ADMIN peut voir n'importe quel user
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        User user = userService.getUserById(id);

        if (user.getUserImage() == null || user.getUserImageContentType() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getUserImageContentType()))
                .body(user.getUserImage());
    }

    // 🔐 ADMIN peut modifier n'importe quel user
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    // 🔐 ADMIN supprime
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    // 👤 Utilisateur connecté voit son profil
    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    // 👤 Utilisateur connecté modifie son profil
    @PutMapping("/me")
    public User updateCurrentUser(
            Authentication authentication,
            @RequestBody UserUpdateRequest request) {

        return userService.updateCurrentUser(authentication.getName(), request);
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> registerWithImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "cin", required = false) String cin,
            @RequestParam(value = "role", required = false) String role) {

        UserRequest request = new UserRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setCin(cin);
        if (role != null && !role.isBlank()) {
            request.setRole(parseRole(role));
        }

        return ResponseEntity.ok(userService.register(request, file));
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    private Role parseRole(String role) {
        for (Role value : Role.values()) {
            if (value.name().equalsIgnoreCase(role)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + role);
    }
}
