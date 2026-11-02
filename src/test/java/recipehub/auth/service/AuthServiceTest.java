package recipehub.auth.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import recipehub.auth.dto.LoginRequest;
import recipehub.auth.dto.RegisterRequest;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.InvalidCredentialsException;
import recipehub.security.TokenService;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwords;
    @Mock TokenService tokens;
    @InjectMocks AuthService service;
    RegisterRequest request = new RegisterRequest("thanh", "thanh@example.com", "password123", "Thanh");

    @Test void registerSuccessHashesPasswordAndIssuesToken() {
        when(passwords.encode("password123")).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            assertEquals("hashed", user.getPassword());
            user.setId(3L);
            return user;
        });
        when(tokens.issue("thanh", 0L)).thenReturn("jwt");
        var response = service.register(request);
        assertEquals(3L, response.userId());
        assertEquals("jwt", response.accessToken());
    }

    @Test void registerDuplicateUsername() {
        when(users.existsByUsername("thanh")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(request));
        verify(users, never()).save(any());
    }

    @Test void registerDuplicateEmail() {
        when(users.existsByEmail("thanh@example.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(request));
    }

    @Test void loginValid() {
        User user = user();
        when(users.findByUsername("thanh")).thenReturn(Optional.of(user));
        when(passwords.matches("password123", "hash")).thenReturn(true);
        when(tokens.issue("thanh", 0L)).thenReturn("jwt");
        assertEquals("jwt", service.login(new LoginRequest("thanh", "password123")).accessToken());
    }

    @Test void loginMissingUser() {
        assertThrows(InvalidCredentialsException.class,
            () -> service.login(new LoginRequest("missing", "password")));
    }

    @Test void loginInvalidPassword() {
        when(users.findByUsername("thanh")).thenReturn(Optional.of(user()));
        assertThrows(InvalidCredentialsException.class,
            () -> service.login(new LoginRequest("thanh", "wrong")));
    }

    @Test void loginDisabledUser() {
        User user = user();
        user.setEnabled(false);
        when(users.findByUsername("thanh")).thenReturn(Optional.of(user));
        assertThrows(InvalidCredentialsException.class,
            () -> service.login(new LoginRequest("thanh", "password123")));
        verify(passwords, never()).matches(any(), any());
    }

    private User user() {
        User user = new User();
        user.setId(3L);
        user.setUsername("thanh");
        user.setPassword("hash");
        user.setEnabled(true);
        return user;
    }
}
