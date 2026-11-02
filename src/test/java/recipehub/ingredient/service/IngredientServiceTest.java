package recipehub.ingredient.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.ingredient.dto.CreateIngredientRequest;
import recipehub.ingredient.entity.Ingredient;
import recipehub.ingredient.repository.IngredientRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {
    @Mock IngredientRepository repository;
    @InjectMocks IngredientService service;
    private final CreateIngredientRequest request = new CreateIngredientRequest("Thịt gà", "500g");

    @Test void list() {
        when(repository.findAll()).thenReturn(List.of(new Ingredient("Thịt gà", "500g")));
        assertEquals(1, service.getAllIngredients().size());
    }

    @Test void readFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Ingredient("Thịt gà", "500g")));
        assertEquals("Thịt gà", service.getIngredientById(1L).getName());
    }

    @Test void readMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.getIngredientById(9L));
    }

    @Test void create() {
        when(repository.save(any(Ingredient.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals("Thịt gà", service.createIngredient(request).getName());
    }

    @Test void duplicateCreate() {
        when(repository.existsByName("Thịt gà")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.createIngredient(request));
    }

    @Test void updateChanged() {
        Ingredient current = new Ingredient("Bò", "Old");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);
        assertEquals("500g", service.updateIngredient(1L, request).getDescription());
    }

    @Test void updateSameName() {
        Ingredient current = new Ingredient("Thịt gà", "Old");
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);
        service.updateIngredient(1L, request);
        verify(repository, never()).existsByName(any());
    }

    @Test void duplicateUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Ingredient("Bò", "Old")));
        when(repository.existsByName("Thịt gà")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.updateIngredient(1L, request));
    }

    @Test void updateMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.updateIngredient(3L, request));
    }

    @Test void deleteFound() {
        Ingredient existing = new Ingredient("Thịt gà", "500g");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        service.deleteIngredient(1L);
        verify(repository).delete(existing);
    }

    @Test void deleteMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.deleteIngredient(9L));
    }
}
