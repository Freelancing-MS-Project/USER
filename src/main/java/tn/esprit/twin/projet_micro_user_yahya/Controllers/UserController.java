package tn.esprit.twin.projet_micro_user_yahya.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
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

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }
}
