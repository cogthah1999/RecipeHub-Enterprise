package recipehub.recipe.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recipehub.recipe.entity.RecipeIngredient;
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipeIdOrderByIdAsc(Long recipeId);
    void deleteByRecipeId(Long recipeId);
}
