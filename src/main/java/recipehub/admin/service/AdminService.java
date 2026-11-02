package recipehub.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipehub.admin.dto.AdminSummary;
import recipehub.admin.dto.AdminUserResponse;
import recipehub.admin.dto.LockUserRequest;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.category.repository.CategoryRepository;
import recipehub.comment.dto.CommentResponse;
import recipehub.comment.entity.Comment;
import recipehub.comment.repository.CommentRepository;
import recipehub.ingredient.repository.IngredientRepository;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@Service
public class AdminService {
    private final UserRepository users;
    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final IngredientRepository ingredients;
    private final CommentRepository comments;

    public AdminService(UserRepository users, RecipeRepository recipes, CategoryRepository categories,
            IngredientRepository ingredients, CommentRepository comments) {
        this.users = users;
        this.recipes = recipes;
        this.categories = categories;
        this.ingredients = ingredients;
        this.comments = comments;
    }

    @Transactional(readOnly = true)
    public void requireAdmin(String username) {
        User actor = users.findByUsername(username).orElseThrow(() ->
                new AccessDeniedException("Tài khoản không hợp lệ"));
        if (!actor.isEnabled() || !"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên mới được phép truy cập");
        }
    }

    @Transactional(readOnly = true)
    public AdminSummary summary(String username) {
        requireAdmin(username);
        return new AdminSummary(users.count(), recipes.count(), categories.count(),
                ingredients.count(), comments.count());
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users(String username) {
        requireAdmin(username);
        return users.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"))).getContent()
                .stream().map(this::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> recentComments(String username) {
        requireAdmin(username);
        return comments.findTop8ByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserResponse lockUser(String adminUsername, Long id, LockUserRequest request) {
        requireAdmin(adminUsername);
        User target = findManageableUser(adminUsername, id);
        if (!target.isEnabled()) {
            throw new DuplicateResourceException("Tài khoản đã bị khóa");
        }
        target.setEnabled(false);
        target.setLockedAt(LocalDateTime.now());
        target.setLockReason(request.reason().trim());
        target.setLockedBy(adminUsername);
        target.setTokenVersion(target.getTokenVersion() + 1);
        return toUserResponse(users.save(target));
    }

    @Transactional
    public AdminUserResponse unlockUser(String adminUsername, Long id) {
        requireAdmin(adminUsername);
        User target = findManageableUser(adminUsername, id);
        if (target.isEnabled()) {
            throw new DuplicateResourceException("Tài khoản đang hoạt động");
        }
        target.setEnabled(true);
        target.setLockedAt(null);
        target.setLockReason(null);
        target.setLockedBy(null);
        target.setTokenVersion(target.getTokenVersion() + 1);
        return toUserResponse(users.save(target));
    }

    private User findManageableUser(String adminUsername, Long id) {
        User target = users.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + id));
        if (Objects.equals(target.getUsername(), adminUsername) || "ADMIN".equals(target.getRole())) {
            throw new AccessDeniedException("Không thể thay đổi trạng thái tài khoản quản trị viên");
        }
        return target;
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole(), user.isEnabled(), user.getCreatedAt(),
                user.getLockedAt(), user.getLockReason(), user.getLockedBy());
    }

    private CommentResponse toResponse(Comment comment) {
        User author = comment.getUser();
        return new CommentResponse(comment.getId(), comment.getRecipe().getId(), author.getId(),
                author.getUsername(), author.getFullName(), comment.getContent(), comment.getCreatedAt());
    }
}
