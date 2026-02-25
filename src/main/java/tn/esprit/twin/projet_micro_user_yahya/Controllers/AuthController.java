package tn.esprit.twin.projet_micro_user_yahya.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.twin.projet_micro_user_yahya.DTO.auth.AuthResponse;
import tn.esprit.twin.projet_micro_user_yahya.DTO.auth.LoginRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;
import tn.esprit.twin.projet_micro_user_yahya.Repositories.UserRepo;
import tn.esprit.twin.projet_micro_user_yahya.Security.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        httpRequest.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepo.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        String role = user.getRole() == null ? "Client" : user.getRole().name();
        String token = jwtService.generateToken(principal, Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "role", role
        ));

        return ResponseEntity.ok(new AuthResponse(token, "Bearer", principal.getUsername(), role));
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found in session"));

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "sessionId", request.getSession(false) != null ? request.getSession(false).getId() : null,
                "userId", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "authorities", authentication.getAuthorities()
        ));
    }

    @GetMapping("/getUserConnecteById")
    public ResponseEntity<Map<String, Object>> getUserConnecteById(
            @RequestParam(value = "token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        String jwt = token;
        if (jwt == null || jwt.isBlank()) {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
            }
        }

        if (jwt == null || jwt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(Map.of("userId", userId));
    }
}
