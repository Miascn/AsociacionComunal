package sv.asociacion.api.auth;

import java.util.Optional;

public interface UserAuthRepository {
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findById(int id);
    void updateLastAccess(int id);
    void updatePassword(int id, String passwordHash, boolean changeRequired);
}
