package recipehub.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipeHubApiIntegrationTest {
    @Autowired MockMvc mvc;

    private String registerAndGetToken() throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String body = """
            {"username":"u%s","email":"u%s@example.com","password":"pass12345","fullName":"Test User"}
            """.formatted(id, id);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").exists())
            .andReturn().getResponse().getContentAsString();
        return response.split("\"accessToken\":\"")[1].split("\"")[0];
    }

    @Test void publicReadIsAllowedButWritesRequireJwt() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/ingredients")).andExpect(status().isOk());
        mvc.perform(get("/api/recipes")).andExpect(status().isOk());
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
            .content(""" 
                {"name":"NoToken"}
                """)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test void registrationLoginProfileAndAuthenticationErrors() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"nobody","password":"password123"}
                """))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"x","email":"invalid","password":"short"}
                """))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test void categoryIngredientRecipeFullLifecycle() throws Exception {
        String token = registerAndGetToken();
        String auth = "Bearer " + token;
        String categoryJson = mvc.perform(post("/api/categories").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Bữa trưa","description":"Món ngon"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").exists())
            .andReturn().getResponse().getContentAsString();
        String categoryId = getJsonId(categoryJson);
        mvc.perform(post("/api/categories").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Bữa trưa"}
                """))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(post("/api/categories").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":""}
                """))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/categories/999999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/categories/" + categoryId).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Bữa trưa mới"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bữa trưa mới"));

        String ingredientJson = mvc.perform(post("/api/ingredients").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Thịt gà","description":"Tươi"}
                """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ingredientId = getJsonId(ingredientJson);
        mvc.perform(get("/api/ingredients/" + ingredientId)).andExpect(status().isOk());
        mvc.perform(put("/api/ingredients/" + ingredientId).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Thịt gà","description":"Đã rửa"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Đã rửa"));

        String recipeBody = """
            {"name":"Gà chiên","description":"Món ngon","cookingTime":30,"servings":2,
             "instructions":"Chiên","categoryId":%s,
             "ingredients":[{"ingredientId":%s,"quantity":500,"unit":"gram"}]}
            """.formatted(categoryId, ingredientId);
        String recipeJson = mvc.perform(post("/api/recipes").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(recipeBody))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ingredients[0].ingredientName").value("Thịt gà"))
            .andReturn().getResponse().getContentAsString();
        String recipeId = getJsonId(recipeJson);
        mvc.perform(get("/api/recipes/" + recipeId)).andExpect(status().isOk());
        mvc.perform(get("/api/recipes").param("keyword", "chiên")).andExpect(status().isOk());
        mvc.perform(get("/api/recipes").param("categoryId", categoryId)).andExpect(status().isOk());
        mvc.perform(put("/api/recipes/" + recipeId).header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content(recipeBody))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/recipes/" + recipeId).header("Authorization", auth))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/ingredients/" + ingredientId).header("Authorization", auth))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/" + categoryId).header("Authorization", auth))
            .andExpect(status().isNoContent());
    }

    private String getJsonId(String json) {
        return json.split("\"id\":")[1].split("[,}]")[0].trim();
    }
}
