package recipehub.comment.controller;

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
import org.springframework.web.bind.annotation.RestController;
import recipehub.comment.dto.CommentRequest;
import recipehub.comment.dto.CommentResponse;
import recipehub.comment.service.CommentService;

@RestController
@RequestMapping("/api/recipes/{recipeId}/comments")
public class CommentController {
    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long recipeId) {
        return service.list(recipeId);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable Long recipeId,
            @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(recipeId, request, jwt.getSubject()));
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(@PathVariable Long recipeId, @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.update(recipeId, commentId, request, jwt.getSubject());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long recipeId, @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt) {
        service.delete(recipeId, commentId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
