package recipehub.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.user.dto.UserProfile;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        User user = users.findByUsername(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
