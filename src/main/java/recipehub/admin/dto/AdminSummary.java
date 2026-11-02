package recipehub.admin.dto;

public record AdminSummary(long totalUsers, long totalRecipes, long totalCategories,
        long totalIngredients, long totalComments) { }
