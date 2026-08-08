package sv.asociacion.api.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenGenerator implements TokenGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        byte[] value = new byte[32];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
