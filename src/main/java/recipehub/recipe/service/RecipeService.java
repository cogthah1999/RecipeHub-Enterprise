package recipehub.recipe.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recipehub.category.entity.Category;
import recipehub.category.repository.CategoryRepository;
import recipehub.common.exception.ResourceNotFoundException;
import recipehub.ingredient.entity.Ingredient;
import recipehub.ingredient.repository.IngredientRepository;
import recipehub.recipe.dto.RecipeIngredientRequest;
import recipehub.recipe.dto.RecipeIngredientResponse;
import recipehub.recipe.dto.RecipeRequest;
import recipehub.recipe.dto.RecipeResponse;
import recipehub.recipe.entity.Recipe;
import recipehub.recipe.entity.RecipeIngredient;
import recipehub.recipe.repository.RecipeIngredientRepository;
import recipehub.recipe.repository.RecipeRepository;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;

@Service
public class RecipeService {
    private final RecipeRepository recipes;
    private final RecipeIngredientRepository recipeIngredients;
    private final CategoryRepository categories;
    private final IngredientRepository ingredients;
    private final UserRepository users;

    public RecipeService(RecipeRepository recipes, RecipeIngredientRepository recipeIngredients,
            CategoryRepository categories, IngredientRepository ingredients, UserRepository users) {
        this.recipes = recipes;
        this.recipeIngredients = recipeIngredients;
        this.categories = categories;
        this.ingredients = ingredients;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RecipeResponse> search(String keyword, Long categoryId) {
        List<Recipe> matches;
        if (keyword != null && !keyword.isBlank()) {
            matches = recipes.findByNameContainingIgnoreCase(keyword.trim());
        } else if (categoryId != null) {
            matches = recipes.findByCategoryId(categoryId);
        } else {
            matches = recipes.findAll();
        }
        if (categoryId != null && keyword != null && !keyword.isBlank()) {
            matches = matches.stream().filter(r -> categoryId.equals(r.getCategory().getId())).toList();
        }
        return matches.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecipeResponse getById(Long id) { return toResponse(findRecipe(id)); }

    @Transactional
    public RecipeResponse create(RecipeRequest request, String username) {
        validateUniqueIngredients(request.ingredients());
        Recipe recipe = new Recipe();
        applyFields(recipe, request);
        User user = users.findByUsername(username).orElseThrow(() ->
            new ResourceNotFoundException("Người dùng không tồn tại"));
        recipe.setUser(user);
        Recipe saved = recipes.saveAndFlush(recipe);
        saveIngredients(saved, request.ingredients());
        return toResponse(saved);
    }

    @Transactional
    public RecipeResponse update(Long id, RecipeRequest request, String username) {
        validateUniqueIngredients(request.ingredients());
        Recipe recipe = findRecipe(id);
        verifyOwner(recipe, username);
        applyFields(recipe, request);
        recipes.saveAndFlush(recipe);
        recipeIngredients.deleteByRecipeId(id);
        recipeIngredients.flush();
        saveIngredients(recipe, request.ingredients());
        return toResponse(recipe);
    }

    @Transactional
    public void delete(Long id, String username) {
        Recipe recipe = findRecipe(id);
        verifyOwner(recipe, username);
        recipeIngredients.deleteByRecipeId(id);
        recipeIngredients.flush();
        recipes.delete(recipe);
    }

    @Transactional
    public void deleteAsAdmin(Long id) {
        Recipe recipe = findRecipe(id);
        recipeIngredients.deleteByRecipeId(id);
        recipeIngredients.flush();
        recipes.delete(recipe);
    }

    private void verifyOwner(Recipe recipe, String username) {
        if (!recipe.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Only the author may change this recipe");
        }
    }

    private Recipe findRecipe(Long id) {
        return recipes.findById(id).orElseThrow(() ->
            new ResourceNotFoundException("Không tìm thấy công thức với ID: " + id));
    }

    private void applyFields(Recipe recipe, RecipeRequest request) {
        Category category = categories.findById(request.categoryId()).orElseThrow(() ->
            new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.categoryId()));
        recipe.setName(request.name());
        recipe.setDescription(request.description());
        recipe.setCookingTime(request.cookingTime());
        recipe.setServings(request.servings());
        recipe.setInstructions(request.instructions());
        recipe.setImageUrl(request.imageUrl());
        recipe.setCategory(category);
    }

    private void validateUniqueIngredients(List<RecipeIngredientRequest> requests) {
        Set<Long> ids = new HashSet<>();
        for (RecipeIngredientRequest request : requests) {
            if (!ids.add(request.ingredientId())) {
                throw new IllegalArgumentException("Nguyên liệu bị lặp với ID: " + request.ingredientId());
            }
        }
    }

    private void saveIngredients(Recipe recipe, List<RecipeIngredientRequest> requests) {
        for (RecipeIngredientRequest request : requests) {
            Ingredient ingredient = ingredients.findById(request.ingredientId()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + request.ingredientId()));
            recipeIngredients.save(new RecipeIngredient(recipe, ingredient, request.quantity(), request.unit()));
        }
        recipeIngredients.flush();
    }

    private RecipeResponse toResponse(Recipe recipe) {
        List<RecipeIngredientResponse> items = recipeIngredients.findByRecipeIdOrderByIdAsc(recipe.getId())
                .stream().map(item -> new RecipeIngredientResponse(
                    item.getIngredient().getId(), item.getIngredient().getName(),
                    item.getQuantity(), item.getUnit())).toList();
        return new RecipeResponse(recipe.getId(), recipe.getName(), recipe.getDescription(),
            recipe.getCookingTime(), recipe.getServings(), recipe.getInstructions(), recipe.getImageUrl(),
            recipe.getCategory().getId(), recipe.getCategory().getName(),
            recipe.getUser().getId(), recipe.getUser().getUsername(), recipe.getCreatedAt(), items);
    }
}
