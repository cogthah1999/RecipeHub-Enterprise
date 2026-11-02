package recipehub.admin.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recipehub.admin.dto.AdminSummary;
import recipehub.admin.dto.AdminUserResponse;
import recipehub.admin.dto.LockUserRequest;
import recipehub.admin.service.AdminService;
import recipehub.comment.dto.CommentResponse;
import recipehub.comment.service.CommentService;
import recipehub.recipe.service.RecipeService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admins;
    private final CommentService comments;
    private final RecipeService recipes;

    public AdminController(AdminService admins, CommentService comments, RecipeService recipes) {
        this.admins = admins;
        this.comments = comments;
        this.recipes = recipes;
    }

    @GetMapping("/summary")
    public AdminSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return admins.summary(jwt.getSubject());
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@AuthenticationPrincipal Jwt jwt) {
        return admins.users(jwt.getSubject());
    }

    @PatchMapping("/users/{id}/lock")
    public AdminUserResponse lockUser(@PathVariable Long id,
            @Valid @RequestBody LockUserRequest request, @AuthenticationPrincipal Jwt jwt) {
        return admins.lockUser(jwt.getSubject(), id, request);
    }

    @PatchMapping("/users/{id}/unlock")
    public AdminUserResponse unlockUser(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return admins.unlockUser(jwt.getSubject(), id);
    }

    @GetMapping("/comments")
    public List<CommentResponse> recentComments(@AuthenticationPrincipal Jwt jwt) {
        return admins.recentComments(jwt.getSubject());
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        admins.requireAdmin(jwt.getSubject());
        comments.deleteAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        admins.requireAdmin(jwt.getSubject());
        recipes.deleteAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
