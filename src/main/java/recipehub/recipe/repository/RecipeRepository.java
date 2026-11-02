package recipehub.recipe.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import recipehub.recipe.entity.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByNameContainingIgnoreCase(String keyword);
    List<Recipe> findByCategoryId(Long categoryId);
}
