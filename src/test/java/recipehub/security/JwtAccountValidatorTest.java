package recipehub.security;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAccountValidatorTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAccountValidator validator = new JwtAccountValidator(users);

    private Jwt jwt(String username, Object version) {
        var builder = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .subject(username)
                .issuedAt(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(60));
        if (version != null) builder.claim("tokenVersion", version);
        return builder.build();
    }

    @Test void rejectsMissingVersionOrUnknownUser() {
        assertTrue(validator.validate(jwt("user", null)).hasErrors());
        assertTrue(validator.validate(jwt("missing", 0)).hasErrors());
    }

    @Test void rejectsDisabledAccountAndRevokedVersion() {
        User user = new User();
        user.setUsername("user");
        user.setTokenVersion(3);
        when(users.findByUsername("user")).thenReturn(Optional.of(user));
        assertTrue(validator.validate(jwt("user", 2)).hasErrors());
        user.setEnabled(false);
        assertTrue(validator.validate(jwt("user", 3)).hasErrors());
    }

    @Test void acceptsEnabledAccountWithCurrentVersion() {
        User user = new User();
        user.setUsername("user");
        user.setTokenVersion(7);
        when(users.findByUsername("user")).thenReturn(Optional.of(user));
        assertFalse(validator.validate(jwt("user", 7)).hasErrors());
    }
}
