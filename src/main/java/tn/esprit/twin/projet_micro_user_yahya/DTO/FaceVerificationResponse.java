package tn.esprit.twin.projet_micro_user_yahya.DTO;

public record FaceVerificationResponse(Boolean match, Double confidence, String error) {
}
