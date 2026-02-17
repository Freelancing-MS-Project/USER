package tn.esprit.twin.projet_micro_user_yahya.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Services.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(userService.getCurrentUser(keycloakId));
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserUpdateRequest request) {

        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(userService.updateCurrentUser(keycloakId, request));
    }
}

