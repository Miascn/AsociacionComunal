package sv.asociacion.api.auth;

import java.util.Set;

public final class AuthorizationService {
    public void requireAnyRole(AuthPrincipal principal, Set<String> allowedRoles) {
        if (principal == null || !allowedRoles.contains(principal.role())) {
            throw new AuthException(403, "FORBIDDEN", "No tiene permiso para realizar esta operación.");
        }
    }
}
