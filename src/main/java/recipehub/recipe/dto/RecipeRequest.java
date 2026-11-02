package recipehub.recipe.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecipeRequest(
    @NotBlank(message = "Tên món ăn không được để trống") @Size(max = 150) String name,
    String description,
    @NotNull @Min(value = 1, message = "Thời gian nấu phải từ 1 đến 1440 phút")
    @Max(value = 1440, message = "Thời gian nấu phải từ 1 đến 1440 phút") Integer cookingTime,
    @NotNull @Positive @Max(value = 100, message = "Số người ăn tối đa 100") Integer servings,
    @NotBlank(message = "Các bước chế biến không được để trống") String instructions,
    String imageUrl,
    @NotNull @Positive Long categoryId,
    @NotEmpty(message = "Phải nhập ít nhất một nguyên liệu")
    List<@NotNull @Valid RecipeIngredientRequest> ingredients
) { }
