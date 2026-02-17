package tn.esprit.twin.projet_micro_user_yahya.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;

import java.util.Optional;

@Repository

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}