package recipehub.recipe.service;

import java.math.BigDecimal;
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
import recipehub.category.entity.Category;
import recipehub.category.repository.CategoryRepository;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.ingredient.entity.Ingredient;
import recipehub.ingredient.repository.IngredientRepository;
import recipehub.recipe.dto.RecipeIngredientRequest;
import recipehub.recipe.dto.RecipeRequest;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.entity.RecipeIngredient;
import recipehub.recipe.repository.RecipeIngredientRepository;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    @Mock RecipeRepository recipes;
    @Mock RecipeIngredientRepository links;
    @Mock CategoryRepository categories;
    @Mock IngredientRepository ingredients;
    @Mock UserRepository users;
    @InjectMocks RecipeService service;
    Category category;
    User author;
    Ingredient ingredient;
    Recipe recipe;

    @BeforeEach void setup() {
        category = new Category("Món Việt", "VN");
        ReflectionTestUtils.setField(category, "id", 3L);
        author = new User();
        author.setId(7L);
        author.setUsername("thanh");
        ingredient = new Ingredient("Gà", "Chicken");
        ReflectionTestUtils.setField(ingredient, "id", 9L);
        recipe = new Recipe();
        recipe.setId(15L);
        recipe.setName("Gà chiên");
        recipe.setCategory(category);
        recipe.setUser(author);
    }

    private RecipeRequest request(List<RecipeIngredientRequest> items) {
        return new RecipeRequest("Gà chiên", "Món ngon", 30, 2, "Chiên", "", 3L, items);
    }

    private RecipeIngredientRequest item() {
        return new RecipeIngredientRequest(9L, new BigDecimal("500.00"), "gram");
    }

    private void foundRecipe() { when(recipes.findById(15L)).thenReturn(Optional.of(recipe)); }
    private void categoryFound() { when(categories.findById(3L)).thenReturn(Optional.of(category)); }
    private void responseEmpty() { when(links.findByRecipeIdOrderByIdAsc(15L)).thenReturn(List.of()); }

    @Test void listEmpty() {
        assertTrue(service.search(null, null).isEmpty());
        verify(recipes).findAll();
    }

    @Test void keywordSearch() {
        when(recipes.findByNameContainingIgnoreCase("gà")).thenReturn(List.of(recipe));
        responseEmpty();
        assertEquals(1, service.search(" gà ", null).size());
    }

    @Test void categorySearch() {
        when(recipes.findByCategoryId(3L)).thenReturn(List.of(recipe));
        responseEmpty();
        assertEquals(1, service.search(null, 3L).size());
    }

    @Test void keywordAndCategoryFiltersResults() {
        when(recipes.findByNameContainingIgnoreCase("Gà")).thenReturn(List.of(recipe));
        responseEmpty();
        assertEquals(1, service.search("Gà", 3L).size());
        assertTrue(service.search("", 999L).isEmpty());
    }

    @Test void getFoundSerializesIngredients() {
        foundRecipe();
        when(links.findByRecipeIdOrderByIdAsc(15L)).thenReturn(
            List.of(new RecipeIngredient(recipe, ingredient, new BigDecimal("500"), "gram")));
        var result = service.getById(15L);
        assertEquals("thanh", result.username());
        assertEquals("Gà", result.ingredients().getFirst().ingredientName());
    }

    @Test void getMissingReturns404() {
        assertThrows(ResourceNotFoundException.class, () -> service.getById(15L));
    }

    @Test void createSuccessfully() {
        categoryFound();
        when(users.findByUsername("thanh")).thenReturn(Optional.of(author));
        when(ingredients.findById(9L)).thenReturn(Optional.of(ingredient));
        when(recipes.saveAndFlush(any(Recipe.class))).thenAnswer(call -> {
            Recipe saved = call.getArgument(0);
            saved.setId(15L);
            return saved;
        });
        responseEmpty();
        var result = service.create(request(List.of(item())), "thanh");
        assertEquals("Gà chiên", result.name());
        assertEquals(7L, result.userId());
        verify(links).save(any(RecipeIngredient.class));
        verify(links).flush();
    }

    @Test void createWithNoIngredients() {
        categoryFound();
        when(users.findByUsername("thanh")).thenReturn(Optional.of(author));
        when(recipes.saveAndFlush(any(Recipe.class))).thenAnswer(call -> {
            Recipe saved = call.getArgument(0);
            saved.setId(15L);
            return saved;
        });
        responseEmpty();
        assertTrue(service.create(request(List.of()), "thanh").ingredients().isEmpty());
        verify(links, never()).save(any());
    }

    @Test void createMissingCategory() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(List.of()), "thanh"));
    }

    @Test void createMissingUser() {
        categoryFound();
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(List.of()), "noone"));
    }

    @Test void createMissingIngredient() {
        categoryFound();
        when(users.findByUsername("thanh")).thenReturn(Optional.of(author));
        when(recipes.saveAndFlush(any(Recipe.class))).thenAnswer(call -> {
            Recipe saved = call.getArgument(0);
            saved.setId(15L);
            return saved;
        });
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(List.of(item())), "thanh"));
    }

    @Test void duplicateIngredientsAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> service.create(request(List.of(item(), item())), "thanh"));
        verifyNoInteractions(recipes);
    }

    @Test void updateSuccess() {
        foundRecipe();
        categoryFound();
        responseEmpty();
        var response = service.update(15L, request(List.of()), "thanh");
        assertEquals(30, response.cookingTime());
        verify(links).deleteByRecipeId(15L);
        verify(links, times(2)).flush();
    }

    @Test void updateForbiddenForDifferentUser() {
        foundRecipe();
        assertThrows(AccessDeniedException.class, () -> service.update(15L, request(List.of()), "other"));
        verify(links, never()).deleteByRecipeId(any());
    }

    @Test void updateMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.update(15L, request(List.of()), "thanh"));
    }

    @Test void updateDuplicateIngredient() {
        assertThrows(IllegalArgumentException.class,
            () -> service.update(15L, request(List.of(item(), item())), "thanh"));
    }

    @Test void deleteSuccess() {
        foundRecipe();
        service.delete(15L, "thanh");
        verify(links).deleteByRecipeId(15L);
        verify(recipes).delete(recipe);
    }

    @Test void deleteForbidden() {
        foundRecipe();
        assertThrows(AccessDeniedException.class, () -> service.delete(15L, "other"));
        verify(recipes, never()).delete(any());
    }

    @Test void deleteNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.delete(15L, "thanh"));
    }
    @Test void adminCanDeleteRecipeWithoutBeingItsAuthor() {
        foundRecipe();
        service.deleteAsAdmin(15L);
        verify(links).deleteByRecipeId(15L);
        verify(recipes).delete(recipe);
    }

}
