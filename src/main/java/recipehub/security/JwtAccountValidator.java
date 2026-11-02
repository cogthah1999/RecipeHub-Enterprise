package recipehub.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@Component
public class JwtAccountValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository users;

    public JwtAccountValidator(UserRepository users) {
        this.users = users;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Object version = token.getClaims().get("tokenVersion");
        if (token.getSubject() == null || !(version instanceof Number)) {
            return invalid();
        }
        User user = users.findByUsername(token.getSubject()).orElse(null);
        if (user == null || !user.isEnabled()
                || user.getTokenVersion() != ((Number) version).longValue()) {
            return invalid();
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Tài khoản bị khóa hoặc phiên đăng nhập đã bị thu hồi", null));
    }
}
