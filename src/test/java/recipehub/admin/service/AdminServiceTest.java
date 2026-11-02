package recipehub.admin.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import recipehub.category.repository.CategoryRepository;
import recipehub.comment.entity.Comment;
import recipehub.comment.repository.CommentRepository;
import recipehub.ingredient.repository.IngredientRepository;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock UserRepository users;
    @Mock RecipeRepository recipes;
    @Mock CategoryRepository categories;
    @Mock IngredientRepository ingredients;
    @Mock CommentRepository comments;
    @InjectMocks AdminService service;

    private User admin() {
        User user = new User();
        user.setId(5L);
        user.setUsername("admin");
        user.setRole("ADMIN");
        return user;
    }

    @Test void normalAndDisabledUsersCannotAccessAdmin() {
        User normal = new User();normal.setUsername("normal");
        when(users.findByUsername("normal")).thenReturn(Optional.of(normal));
        assertThrows(AccessDeniedException.class, () -> service.summary("normal"));
        assertThrows(AccessDeniedException.class, () -> service.requireAdmin("missing"));
        User disabled = admin();disabled.setEnabled(false);
        when(users.findByUsername("admin")).thenReturn(Optional.of(disabled));
        assertThrows(AccessDeniedException.class, () -> service.users("admin"));
        verifyNoInteractions(recipes, categories, ingredients, comments);
    }

    @Test void adminCanGetCountsAndUserList() {
        User actor = admin();
        when(users.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(users.count()).thenReturn(5L);
        when(recipes.count()).thenReturn(4L);
        when(categories.count()).thenReturn(3L);
        when(ingredients.count()).thenReturn(9L);
        when(comments.count()).thenReturn(12L);
        assertEquals(12, service.summary("admin").totalComments());
        when(users.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(actor)));
        assertEquals("ADMIN", service.users("admin").getFirst().role());
    }

    @Test void adminCanReadRecentComments() {
        User actor = admin();
        Recipe recipe = new Recipe();recipe.setId(21L);
        Comment comment = new Comment();comment.setId(1L);comment.setUser(actor);
        comment.setRecipe(recipe);comment.setContent("Tốt");
        when(users.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(comments.findTop8ByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(comment));
        assertEquals(21L, service.recentComments("admin").getFirst().recipeId());
    }
}
