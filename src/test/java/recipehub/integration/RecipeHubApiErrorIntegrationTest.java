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
class RecipeHubApiErrorIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void malformedJsonIs400AndUnknownRecipesAre404() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("not json")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/recipes/999999999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/ingredients/999999999")).andExpect(status().isNotFound());
    }

    @Test void duplicateRegistrationAndLogin() throws Exception {
        String name = "user" + UUID.randomUUID().toString().substring(0, 8);
        String payload = """
            {"username":"%s","email":"%s@example.com","password":"strongpassword","fullName":"Test user"}
            """.formatted(name, name);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"strongpassword"}
                """.formatted(name)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"wrongpass"}
                """.formatted(name)))
            .andExpect(status().isUnauthorized());
    }
}
