package recipehub.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import recipehub.auth.dto.AuthResponse;
import recipehub.auth.dto.LoginRequest;
import recipehub.auth.dto.RegisterRequest;
import recipehub.auth.service.AuthService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    @Test void registerReturns201AndLoginReturnsToken() {
        AuthService auth = mock(AuthService.class);
        AuthController controller = new AuthController(auth);
        RegisterRequest register = new RegisterRequest("thanh", "thanh@example.com", "password123", "Thanh");
        LoginRequest login = new LoginRequest("thanh", "password123");
        AuthResponse result = new AuthResponse("Bearer", "JWT", 1L, "thanh");
        when(auth.register(register)).thenReturn(result);
        when(auth.login(login)).thenReturn(result);
        assertEquals(HttpStatus.CREATED, controller.register(register).getStatusCode());
        assertEquals("JWT", controller.login(login).accessToken());
    }
}
