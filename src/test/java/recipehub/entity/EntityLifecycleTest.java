package recipehub.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import recipehub.category.entity.Category;
import recipehub.ingredient.entity.Ingredient;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.entity.RecipeIngredient;
import recipehub.user.entity.User;
import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {
    @Test void categoryLifecycleAndAccessors() {
        Category c = new Category("Soup", "Good");
        ReflectionTestUtils.setField(c, "id", 12L);
        assertEquals(12L, c.getId());
        c.setName("Salad"); c.setDescription("Fresh");
        assertEquals("Salad", c.getName());
        assertEquals("Fresh", c.getDescription());
        ReflectionTestUtils.invokeMethod(c, "onCreate");
        assertNotNull(c.getCreatedAt());
    }

    @Test void ingredientAccessors() {
        Ingredient i = new Ingredient("Gà", "Chicken");
        ReflectionTestUtils.setField(i, "id", 33L);
        assertEquals(33L, i.getId());
        i.setName("Bò"); i.setDescription("Beef");
        assertEquals("Bò", i.getName()); assertEquals("Beef", i.getDescription());
    }

    @Test void recipeAndLinkLifecycle() {
        Recipe recipe = new Recipe();
        User user = new User();
        Category category = new Category("Soup", "Good");
        recipe.setId(7L); recipe.setName("Gà chiên"); recipe.setDescription("Thử");
        recipe.setCookingTime(25); recipe.setServings(3); recipe.setInstructions("Fry");
        recipe.setImageUrl("https://example.com/image.png"); recipe.setCategory(category); recipe.setUser(user);
        ReflectionTestUtils.invokeMethod(recipe, "onCreate");
        assertEquals(7L, recipe.getId()); assertEquals("Gà chiên", recipe.getName());
        assertEquals("Thử", recipe.getDescription()); assertEquals(25, recipe.getCookingTime());
        assertEquals(3, recipe.getServings()); assertEquals("Fry", recipe.getInstructions());
        assertTrue(recipe.getImageUrl().startsWith("https:"));
        assertSame(category, recipe.getCategory()); assertSame(user, recipe.getUser());
        assertNotNull(recipe.getCreatedAt());
        Ingredient ingredient = new Ingredient("Meat", "x");
        RecipeIngredient link = new RecipeIngredient(recipe, ingredient, new BigDecimal("500.00"), "gram");
        ReflectionTestUtils.setField(link, "id", 10L);
        assertEquals(10L, link.getId()); assertSame(recipe, link.getRecipe());
        assertSame(ingredient, link.getIngredient());
        link.setQuantity(new BigDecimal("300.00")); link.setUnit("ml");
        assertEquals(new BigDecimal("300.00"), link.getQuantity());
        assertEquals("ml", link.getUnit());
    }

    @Test void userLifecycleAndAccessors() {
        User user = new User();
        user.setId(4L); user.setUsername("thanh"); user.setEmail("test@example.com");
        user.setPassword("encrypted"); user.setFullName("Thanh"); user.setEnabled(false);
        ReflectionTestUtils.invokeMethod(user, "onCreate");
        assertEquals(4L, user.getId()); assertEquals("thanh", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("encrypted", user.getPassword()); assertEquals("Thanh", user.getFullName());
        assertFalse(user.isEnabled()); assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        ReflectionTestUtils.invokeMethod(user, "onUpdate");
        assertNotNull(user.getUpdatedAt());
    }
}
