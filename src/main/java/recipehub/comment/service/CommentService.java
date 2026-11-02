package recipehub.comment.service;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipehub.comment.dto.CommentRequest;
import recipehub.comment.dto.CommentResponse;
import recipehub.comment.entity.Comment;
import recipehub.comment.repository.CommentRepository;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final RecipeRepository recipes;
    private final UserRepository users;

    public CommentService(CommentRepository comments, RecipeRepository recipes, UserRepository users) {
        this.comments = comments;
        this.recipes = recipes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long recipeId) {
        findRecipe(recipeId);
        return comments.findByRecipeIdOrderByCreatedAtDescIdDesc(recipeId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CommentResponse create(Long recipeId, CommentRequest request, String username) {
        Recipe recipe = findRecipe(recipeId);
        User user = findUser(username);
        Comment comment = new Comment();
        comment.setRecipe(recipe);
        comment.setUser(user);
        comment.setContent(request.content().trim());
        return toResponse(comments.saveAndFlush(comment));
    }

    @Transactional
    public CommentResponse update(Long recipeId, Long commentId, CommentRequest request, String username) {
        Comment comment = findCommentOnRecipe(recipeId, commentId);
        authorizeOwnerOrAdmin(comment, username);
        comment.setContent(request.content().trim());
        return toResponse(comments.saveAndFlush(comment));
    }

    @Transactional
    public void delete(Long recipeId, Long commentId, String username) {
        Comment comment = findCommentOnRecipe(recipeId, commentId);
        authorizeOwnerOrAdmin(comment, username);
        comments.delete(comment);
    }

    @Transactional
    public void deleteAsAdmin(Long commentId) {
        Comment comment = comments.findById(commentId).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy bình luận với ID: " + commentId));
        comments.delete(comment);
    }

    private Recipe findRecipe(Long id) {
        return recipes.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));
    }

    private User findUser(String username) {
        User user = users.findByUsername(username).orElseThrow(() ->
                new AccessDeniedException("Tài khoản không tồn tại"));
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Tài khoản đã bị vô hiệu hóa");
        }
        return user;
    }

    private Comment findCommentOnRecipe(Long recipeId, Long commentId) {
        return comments.findById(commentId).filter(c -> c.getRecipe().getId().equals(recipeId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận với ID: " + commentId));
    }

    private void authorizeOwnerOrAdmin(Comment comment, String username) {
        User actor = findUser(username);
        if (!actor.getId().equals(comment.getUser().getId()) && !"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Chỉ tác giả hoặc Admin được thay đổi bình luận");
        }
    }

    private CommentResponse toResponse(Comment comment) {
        User author = comment.getUser();
        return new CommentResponse(comment.getId(), comment.getRecipe().getId(), author.getId(),
                author.getUsername(), author.getFullName(), comment.getContent(), comment.getCreatedAt());
    }
}
