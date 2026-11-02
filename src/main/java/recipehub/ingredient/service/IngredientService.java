package recipehub.ingredient.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.ingredient.dto.CreateIngredientRequest;
import recipehub.ingredient.entity.Ingredient;
import recipehub.ingredient.repository.IngredientRepository;

@Service
public class IngredientService {
    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Transactional(readOnly = true)
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Ingredient getIngredientById(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nguyên liệu với ID: " + id));
    }

    @Transactional
    public Ingredient createIngredient(CreateIngredientRequest request) {
        if (ingredientRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tên nguyên liệu đã tồn tại");
        }
        Ingredient ingredient = new Ingredient(request.name(), request.description());
        return ingredientRepository.save(ingredient);
    }

    @Transactional
    public Ingredient updateIngredient(Long id, CreateIngredientRequest request) {
        Ingredient ingredient = getIngredientById(id);
        if (!ingredient.getName().equals(request.name())
                && ingredientRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tên nguyên liệu đã tồn tại");
        }
        ingredient.setName(request.name());
        ingredient.setDescription(request.description());
        return ingredientRepository.save(ingredient);
    }

    @Transactional
    public void deleteIngredient(Long id) {
        Ingredient ingredient = getIngredientById(id);
        ingredientRepository.delete(ingredient);
        ingredientRepository.flush();
    }
}
