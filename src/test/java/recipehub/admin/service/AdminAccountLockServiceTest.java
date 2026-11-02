package recipehub.admin.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import recipehub.admin.dto.LockUserRequest;
import recipehub.category.repository.CategoryRepository;
import recipehub.comment.repository.CommentRepository;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.ingredient.repository.IngredientRepository;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountLockServiceTest {
    @Mock UserRepository users;
    @Mock RecipeRepository recipes;
    @Mock CategoryRepository categories;
    @Mock IngredientRepository ingredients;
    @Mock CommentRepository comments;
    @InjectMocks AdminService service;

    private User account(long id, String name, String role) {
        User u = new User(); u.setId(id); u.setUsername(name); u.setRole(role);
        return u;
    }

    private void admin() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(account(1L, "admin", "ADMIN")));
    }

    @Test void adminLocksAccountWithReasonAndRevokesPreviousTokens() {
        admin();
        User target = account(2L, "cook", "USER");
        target.setTokenVersion(7);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        var result = service.lockUser("admin", 2L, new LockUserRequest("  Spam  "));
        assertFalse(result.enabled());
        assertEquals("Spam", result.lockReason());
        assertEquals("admin", result.lockedBy());
        assertNotNull(result.lockedAt());
        assertEquals(8, target.getTokenVersion());
    }

    @Test void unlockingClearsReasonAndRevokesOldTokensAgain() {
        admin();
        User target = account(2L, "cook", "USER");
        target.setEnabled(false);
        target.setTokenVersion(4);
        target.setLockedBy("admin"); target.setLockReason("Spam");
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        var result = service.unlockUser("admin", 2L);
        assertTrue(result.enabled());
        assertNull(result.lockReason());
        assertNull(result.lockedBy());
        assertEquals(5, target.getTokenVersion());
    }

    @Test void rejectsUnknownAccountAndSelfOrAdminLock() {
        admin();
        assertThrows(ResourceNotFoundException.class,
                () -> service.lockUser("admin", 404L, new LockUserRequest("Spam")));
        when(users.findById(1L)).thenReturn(Optional.of(account(1L, "admin", "ADMIN")));
        assertThrows(AccessDeniedException.class,
                () -> service.lockUser("admin", 1L, new LockUserRequest("Spam")));
        when(users.findById(3L)).thenReturn(Optional.of(account(3L, "otheradmin", "ADMIN")));
        assertThrows(AccessDeniedException.class,
                () -> service.unlockUser("admin", 3L));
    }

    @Test void rejectsRepeatedLockOrUnlock() {
        admin();
        User target = account(2L, "cook", "USER");
        target.setEnabled(false);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        assertThrows(DuplicateResourceException.class,
                () -> service.lockUser("admin", 2L, new LockUserRequest("Spam")));
        target.setEnabled(true);
        assertThrows(DuplicateResourceException.class,
                () -> service.unlockUser("admin", 2L));
        verify(users, never()).save(any(User.class));
    }

    @Test void nonAdminCannotChangeAccounts() {
        User normal = account(3L, "normal", "USER");
        when(users.findByUsername("normal")).thenReturn(Optional.of(normal));
        assertThrows(AccessDeniedException.class,
                () -> service.lockUser("normal", 2L, new LockUserRequest("Spam")));
        verify(users, never()).findById(anyLong());
    }
}
