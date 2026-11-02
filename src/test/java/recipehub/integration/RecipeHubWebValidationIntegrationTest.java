package recipehub.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipeHubWebValidationIntegrationTest {
    @Autowired MockMvc mvc;

    private String authHeader() throws Exception {
        String id = "web" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
            {"username":"%s","email":"%s@example.com",
             "password":"StrongPassword123!","fullName":"Web Tester"}
            """.formatted(id, id);
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + response.split("\"accessToken\":\"")[1].split("\"")[0];
    }

    @Test void guestsMayReadRecipesAndVisitAllPublicPages() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login.html")).andExpect(status().isOk());
        mvc.perform(get("/register.html")).andExpect(status().isOk());
        mvc.perform(get("/recipe.html")).andExpect(status().isOk());
        mvc.perform(get("/editor.html")).andExpect(status().isOk());
        mvc.perform(get("/api/recipes")).andExpect(status().isOk());
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
            .content("{}") ).andExpect(status().isUnauthorized());
    }

    @Test void blankInstructionsAreRejectedWithReadable400() throws Exception {
        mvc.perform(post("/api/recipes").header("Authorization", authHeader())
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Món ngon","cookingTime":30,"servings":2,
                 "instructions":" ","categoryId":1,
                 "ingredients":[{"ingredientId":1,"quantity":100,"unit":"gram"}]}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Các bước chế biến không được để trống"));
    }

    @Test void emptyIngredientsAreRejectedWithReadable400() throws Exception {
        mvc.perform(post("/api/recipes").header("Authorization", authHeader())
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Món ngon","cookingTime":30,"servings":2,
                 "instructions":"Sơ chế và nấu","categoryId":1,"ingredients":[]}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Phải nhập ít nhất một nguyên liệu"));
    }

    @Test void nonpositiveQuantityIsRejected() throws Exception {
        mvc.perform(post("/api/recipes").header("Authorization", authHeader())
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Món ngon","cookingTime":30,"servings":2,
                 "instructions":"Sơ chế và nấu","categoryId":1,
                 "ingredients":[{"ingredientId":1,"quantity":0,"unit":"gram"}]}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test void registerRequiresFullName() throws Exception {
        String id = "web" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","email":"%s@example.com",
                 "password":"StrongPassword123!","fullName":" "}
                """.formatted(id, id)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Họ tên không được để trống"));
    }
}
