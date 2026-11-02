package recipehub.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import recipehub.user.entity.User;
import recipehub.user.repository.UserRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipeHubCommentsAdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private record Account(String username, String jwt) { }

    private Account register(String label) throws Exception {
        String username = label + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String body = """
            {"username":"%s","email":"%s@example.com","password":"strongpassword","fullName":"Test User"}
            """.formatted(username, username);
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Account(username, json.split("\"accessToken\":\"")[1].split("\"")[0]);
    }

    private String bearer(Account account) { return "Bearer " + account.jwt(); }

    private String idOf(String json) {
        return json.split("\"id\":")[1].split("[,}]")[0].trim();
    }

    private String createRecipe(Account author) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String cat = mvc.perform(post("/api/categories")
            .header("Authorization", bearer(author)).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Nhóm %s","description":"Món ngon"}
                """.formatted(suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ingredient = mvc.perform(post("/api/ingredients")
            .header("Authorization", bearer(author)).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Nguyên liệu %s","description":"Tươi"}
                """.formatted(suffix)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String recipe = mvc.perform(post("/api/recipes")
            .header("Authorization", bearer(author)).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Món thử nghiệm","cookingTime":20,"servings":2,
                 "instructions":"Sơ chế nguyên liệu và chiên đến khi chín","categoryId":%s,
                 "ingredients":[{"ingredientId":%s,"quantity":500,"unit":"gram"}]}
                """.formatted(idOf(cat), idOf(ingredient))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return idOf(recipe);
    }

    private String comment(String recipeId, Account actor, String content) throws Exception {
        String json = mvc.perform(post("/api/recipes/" + recipeId + "/comments")
            .header("Authorization", bearer(actor)).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content":"%s"}
                """.formatted(content)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value(actor.username()))
            .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    @Test void guestsCanReadButCannotPostCommentsAndValidationWorks() throws Exception {
        Account author = register("chef");
        String recipeId = createRecipe(author);
        mvc.perform(get("/api/recipes/" + recipeId + "/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/recipes/" + recipeId + "/comments")
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Hello\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/recipes/" + recipeId + "/comments")
            .header("Authorization", bearer(author)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"   \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/recipes/99999999/comments"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void commentAuthorsCanEditDeleteAndOthersCannot() throws Exception {
        Account author = register("owner");
        Account other = register("other");
        String recipeId = createRecipe(author);
        String commentId = comment(recipeId, author, "Ngon quá");
        mvc.perform(get("/api/recipes/" + recipeId + "/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("Ngon quá"));
        mvc.perform(put("/api/recipes/" + recipeId + "/comments/" + commentId)
            .header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Viết lại\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/recipes/" + recipeId + "/comments/" + commentId)
            .header("Authorization", bearer(other))).andExpect(status().isForbidden());
        mvc.perform(put("/api/recipes/" + recipeId + "/comments/" + commentId)
            .header("Authorization", bearer(author)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Rất ngon\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Rất ngon"));
        mvc.perform(delete("/api/recipes/" + recipeId + "/comments/" + commentId)
            .header("Authorization", bearer(author))).andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/" + recipeId + "/comments"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void onlyDatabasePromotedAdminsCanSeeDashboardAndModerate() throws Exception {
        Account author = register("owner");
        Account regular = register("normal");
        Account admin = register("admin");
        String recipeId = createRecipe(author);
        String commentId = comment(recipeId, author, "Kiểm tra dashboard");
        mvc.perform(get("/api/admin/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/summary").header("Authorization", bearer(regular)))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/comments/" + commentId)
            .header("Authorization", bearer(regular))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/recipes/" + recipeId)
            .header("Authorization", bearer(regular))).andExpect(status().isForbidden());

        User operator = users.findByUsername(admin.username()).orElseThrow();
        operator.setRole("ADMIN");
        users.saveAndFlush(operator);
        mvc.perform(get("/api/users/me").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/admin/summary").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRecipes").isNumber())
            .andExpect(jsonPath("$.totalComments").isNumber());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(get("/api/admin/comments").header("Authorization", bearer(admin)))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/comments/" + commentId)
            .header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        comment(recipeId, author, "Bình luận mới");
        mvc.perform(delete("/api/admin/recipes/" + recipeId)
            .header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/" + recipeId)).andExpect(status().isNotFound());
    }

    @Test void deletingOwnRecipeWithCommentsRemovesChildrenAndDisabledAdminIsDenied() throws Exception {
        Account author = register("owner");
        Account admin = register("admin");
        User operator = users.findByUsername(admin.username()).orElseThrow();
        operator.setRole("ADMIN");
        operator.setEnabled(false);
        users.saveAndFlush(operator);
        mvc.perform(get("/api/admin/summary").header("Authorization", bearer(admin)))
            .andExpect(status().isUnauthorized());
        String recipeId = createRecipe(author);
        comment(recipeId, author, "Bình luận trước khi xóa");
        mvc.perform(delete("/api/recipes/" + recipeId).header("Authorization", bearer(author)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/" + recipeId + "/comments"))
            .andExpect(status().isNotFound());
    }
}
