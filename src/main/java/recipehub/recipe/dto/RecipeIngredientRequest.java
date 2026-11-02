package recipehub.recipe.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
public record RecipeIngredientRequest(
    @NotNull @Positive Long ingredientId,
    @NotNull @DecimalMin(value="0.01",message="Số lượng phải lớn hơn 0") @Digits(integer=8,fraction=2) BigDecimal quantity,
    @NotBlank(message = "Đơn vị nguyên liệu không được để trống") @Size(max = 30) String unit
) {}
