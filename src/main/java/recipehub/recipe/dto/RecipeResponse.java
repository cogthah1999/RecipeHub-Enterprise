package recipehub.recipe.dto;
import java.time.LocalDateTime;
import java.util.List;
public record RecipeResponse(
    Long id, String name, String description, Integer cookingTime, Integer servings,
    String instructions, String imageUrl, Long categoryId, String categoryName,
    Long userId, String username, LocalDateTime createdAt,
    List<RecipeIngredientResponse> ingredients
) {}
