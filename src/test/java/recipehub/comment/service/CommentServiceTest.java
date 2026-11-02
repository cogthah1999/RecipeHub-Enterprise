package recipehub.comment.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import recipehub.comment.dto.CommentRequest;
import recipehub.comment.entity.Comment;
import recipehub.comment.repository.CommentRepository;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentRepository comments;
    @Mock RecipeRepository recipes;
    @Mock UserRepository users;
    @InjectMocks CommentService service;
    private Recipe recipe;
    private Comment comment;
    private User author;

    @BeforeEach void init() {
        recipe = new Recipe();
        recipe.setId(1L);
        author = new User();
        author.setId(2L);
        author.setUsername("chef");
        author.setFullName("Chef A");
        comment = new Comment();
        comment.setId(3L);
        comment.setRecipe(recipe);
        comment.setUser(author);
        comment.setContent("Ngon quá");
    }

    @Test void listAndNotFound() {
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        when(comments.findByRecipeIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(comment));
        assertEquals("Ngon quá", service.list(1L).getFirst().content());
        assertThrows(ResourceNotFoundException.class, () -> service.list(99L));
    }

    @Test void createsCommentForValidUser() {
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        when(users.findByUsername("chef")).thenReturn(Optional.of(author));
        when(comments.saveAndFlush(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Thử nấu", service.create(1L, new CommentRequest(" Thử nấu "), "chef").content());
    }

    @Test void disabledOrUnknownUserCannotPost() {
        when(recipes.findById(1L)).thenReturn(Optional.of(recipe));
        assertThrows(AccessDeniedException.class,
                () -> service.create(1L, new CommentRequest("Hi"), "notthere"));
        author.setEnabled(false);
        when(users.findByUsername("chef")).thenReturn(Optional.of(author));
        assertThrows(AccessDeniedException.class,
                () -> service.create(1L, new CommentRequest("Hi"), "chef"));
    }

    @Test void ownersAndAdminsCanModify() {
        when(comments.findById(3L)).thenReturn(Optional.of(comment));
        when(users.findByUsername("chef")).thenReturn(Optional.of(author));
        when(comments.saveAndFlush(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Mới", service.update(1L, 3L, new CommentRequest(" Mới "), "chef").content());
        service.delete(1L, 3L, "chef");
        verify(comments).delete(comment);
        User admin = new User();
        admin.setId(8L);admin.setUsername("admin");admin.setRole("ADMIN");
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        service.delete(1L, 3L, "admin");
        verify(comments, times(2)).delete(comment);
    }

    @Test void nonOwnerDeniedAndWrongRecipeNotFound() {
        when(comments.findById(3L)).thenReturn(Optional.of(comment));
        User other = new User();other.setId(7L);other.setUsername("other");
        when(users.findByUsername("other")).thenReturn(Optional.of(other));
        assertThrows(AccessDeniedException.class, () -> service.delete(1L, 3L, "other"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(5L, 3L, new CommentRequest("Nội dung"), "other"));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteAsAdmin(99L));
    }

    @Test void adminCanDeleteAnyComment() {
        when(comments.findById(3L)).thenReturn(Optional.of(comment));
        service.deleteAsAdmin(3L);
        verify(comments).delete(comment);
    }
}
