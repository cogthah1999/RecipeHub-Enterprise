package recipehub.recipe.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import recipehub.recipe.dto.RecipeRequest;
import recipehub.recipe.dto.RecipeResponse;
import recipehub.recipe.service.RecipeService;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService service;
    public RecipeController(RecipeService service) { this.service = service; }

    @GetMapping
    public List<RecipeResponse> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return service.search(keyword, categoryId);
    }

    @GetMapping("/{id}")
    public RecipeResponse get(@PathVariable Long id) { return service.getById(id); }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecipeRequest request) {
        return service.update(id, request, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
