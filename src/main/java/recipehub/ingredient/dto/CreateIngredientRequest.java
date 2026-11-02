package recipehub.ingredient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIngredientRequest(
        @NotBlank(message = "Tên nguyên liệu không được để trống")
        @Size(max = 100, message = "Tên nguyên liệu tối đa 100 ký tự")
        String name,
        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description
) {}
