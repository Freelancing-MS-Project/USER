package tn.esprit.twin.projet_micro_user_yahya.Services;

import tn.esprit.twin.projet_micro_user_yahya.DTO.UserRequest;
import tn.esprit.twin.projet_micro_user_yahya.DTO.UserUpdateRequest;
import tn.esprit.twin.projet_micro_user_yahya.Entities.User;

import java.util.List;

public interface IUserService {

    User register(UserRequest request);

    User getCurrentUser(String keycloakId);

    User updateCurrentUser(String keycloakId, UserUpdateRequest request);

    List<User> getAllUsers();

    void deleteUser(Long id);
}
