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
class RecipeHubOwnershipIntegrationTest {
    @Autowired MockMvc mvc;

    private String token(String label) throws Exception {
        String id = label + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String payload = """
            {"username":"%s","email":"%s@example.com","password":"strongpassword","fullName":"Test user"}
            """.formatted(id, id);
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.split("\"accessToken\":\"")[1].split("\"")[0];
    }

    private String idOf(String json) {
        return json.split("\"id\":")[1].split("[,}]")[0].trim();
    }

    @Test void onlyAuthorCanEditOrDeleteRecipe() throws Exception {
        String author = "Bearer " + token("owner");
        String other = "Bearer " + token("other");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String category = mvc.perform(post("/api/categories").header("Authorization", author)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Test %s","description":"ownership"}
                """.formatted(suffix)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ingredient = mvc.perform(post("/api/ingredients").header("Authorization", author)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Ingredient %s","description":"ownership"}
                """.formatted(suffix)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String payload = """
            {"name":"Recipe ownership","cookingTime":20,"servings":2,
             "instructions":"Test","categoryId":%s,
             "ingredients":[{"ingredientId":%s,"quantity":1,"unit":"gram"}]}
            """.formatted(idOf(category), idOf(ingredient));
        String recipe = mvc.perform(post("/api/recipes").header("Authorization", author)
            .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = idOf(recipe);
        mvc.perform(put("/api/recipes/" + id).header("Authorization", other)
            .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(delete("/api/recipes/" + id).header("Authorization", other))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/recipes/" + id)).andExpect(status().isOk());
        mvc.perform(delete("/api/recipes/" + id).header("Authorization", author))
            .andExpect(status().isNoContent());
    }

    @Test void invalidBearerTokenIsRejected() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", "Bearer invalid.token")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Disallowed"}
                """))
            .andExpect(status().isUnauthorized());
    }
}
