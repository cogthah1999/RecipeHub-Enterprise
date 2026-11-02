package recipehub.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import recipehub.ingredient.entity.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    boolean existsByName(String name);
}
