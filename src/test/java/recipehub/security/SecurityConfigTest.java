package recipehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test void bcryptEncoderHashesPassword() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
        String hash = encoder.encode("StrongPassword123!");
        assertNotEquals("StrongPassword123!", hash);
        assertTrue(encoder.matches("StrongPassword123!", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test void weakJwtKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SecurityConfig().jwtKey("short"));
    }

    @Test void strongJwtKeyIsAccepted() {
        var config = new SecurityConfig();
        var key = config.jwtKey("abcdefghijklmnopqrstuvwxyz0123456789");
        assertNotNull(config.jwtEncoder(key));
        assertNotNull(config.jwtDecoder(key, new JwtAccountValidator(
                org.mockito.Mockito.mock(recipehub.user.repository.UserRepository.class))));
    }
}
