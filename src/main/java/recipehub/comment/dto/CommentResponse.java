package recipehub.comment.dto;

import java.time.LocalDateTime;

public record CommentResponse(Long id, Long recipeId, Long userId,
        String username, String fullName, String content, LocalDateTime createdAt) { }
