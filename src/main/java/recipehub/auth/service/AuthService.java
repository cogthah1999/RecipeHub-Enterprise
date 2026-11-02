package recipehub.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipehub.auth.dto.AuthResponse;
import recipehub.auth.dto.LoginRequest;
import recipehub.auth.dto.RegisterRequest;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.InvalidCredentialsException;
import recipehub.security.TokenService;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Tên người dùng đã tồn tại");
        }
        if (users.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwords.encode(request.password()));
        user.setFullName(request.fullName());
        User saved = users.save(user);
        return new AuthResponse("Bearer", tokens.issue(saved.getUsername(), saved.getTokenVersion()), saved.getId(), saved.getUsername());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng"));
        if (!user.isEnabled() || !passwords.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
        return new AuthResponse("Bearer", tokens.issue(user.getUsername(), user.getTokenVersion()), user.getId(), user.getUsername());
    }
}
