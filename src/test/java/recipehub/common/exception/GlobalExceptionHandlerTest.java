package recipehub.common.exception;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void duplicateIs409() {
        var response = handler.duplicate(new DuplicateResourceException("duplicate"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("duplicate", response.getBody().message());
    }

    @Test void missingIs404() {
        var response = handler.notFound(new ResourceNotFoundException("missing"));
        assertEquals(404, response.getBody().status());
    }

    @Test void invalidCredentialsAre401() {
        var response = handler.invalidCredentials(new InvalidCredentialsException("invalid"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test void wrongOwnerIs403() {
        var response = handler.forbidden(new AccessDeniedException("no"));
        assertEquals(403, response.getBody().status());
    }

    @Test void malformedInputIs400() {
        var response = handler.badInput(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test void dbIntegrityIs409() {
        var response = handler.integrity(new DataIntegrityViolationException("FK"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test void validationPicksFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult errors = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(errors);
        when(errors.getFieldErrors()).thenReturn(List.of(new FieldError("request", "name", "Tên trống")));
        assertEquals("Tên trống", handler.validation(ex).getBody().message());
    }

    @Test void validationFallbackWhenNoFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult errors = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(errors);
        when(errors.getFieldErrors()).thenReturn(List.of());
        assertEquals("Dữ liệu không hợp lệ", handler.validation(ex).getBody().message());
    }
}
