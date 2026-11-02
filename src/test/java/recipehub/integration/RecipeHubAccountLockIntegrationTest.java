package recipehub.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import recipehub.user.repository.UserRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipeHubAccountLockIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private record Account(String username, String token, Long id) { }

    private Account register(String label) throws Exception {
        String username = label + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        String body = """
            {"username":"%s","email":"%s@example.com","password":"strongpassword","fullName":"Test User"}
            """.formatted(username, username);
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = users.findByUsername(username).orElseThrow().getId();
        return new Account(username, response.split("\"accessToken\":\"")[1].split("\"")[0], id);
    }

    private String bearer(Account account) { return "Bearer " + account.token(); }

    private String lockPath(Account user) { return "/api/admin/users/" + user.id() + "/lock"; }

    @Test void lockAndUnlockRevokeOldJwtAndAdminIsProtected() throws Exception {
        Account admin = register("operator");
        Account member = register("member");
        Account other = register("other");
        var operator = users.findByUsername(admin.username()).orElseThrow();
        operator.setRole("ADMIN");
        users.saveAndFlush(operator);

        mvc.perform(get("/api/users/me").header("Authorization", bearer(member)))
                .andExpect(status().isOk());
        mvc.perform(patch(lockPath(member)).header("Authorization", bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Spam\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(lockPath(member)).header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(lockPath(member)).header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Spam links\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.lockReason").value("Spam links"));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(member)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"strongpassword\"}".formatted(member.username())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recipes")).andExpect(status().isOk());
        mvc.perform(patch(lockPath(member)).header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Again\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/users/" + member.id() + "/unlock")
                .header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(member)))
                .andExpect(status().isUnauthorized());
        String loginResponse = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"username":"%s","password":"strongpassword"}
                    """.formatted(member.username())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newToken = loginResponse.split("\"accessToken\":\"")[1].split("\"")[0];
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/users/" + member.id() + "/unlock")
                .header("Authorization", bearer(admin))).andExpect(status().isConflict());
    }

    @Test void adminCannotLockOwnOrAnotherAdminsAccount() throws Exception {
        Account admin = register("admin");
        Account admin2 = register("admin2");
        var first = users.findByUsername(admin.username()).orElseThrow();
        first.setRole("ADMIN"); users.saveAndFlush(first);
        var second = users.findByUsername(admin2.username()).orElseThrow();
        second.setRole("ADMIN"); users.saveAndFlush(second);
        mvc.perform(patch(lockPath(admin)).header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(lockPath(admin2)).header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/99999999/lock")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}"))
                .andExpect(status().isNotFound());
    }
}
