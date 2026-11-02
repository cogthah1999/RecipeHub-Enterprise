package recipehub.admin.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(Long id, String username, String email,
        String fullName, String role, boolean enabled, LocalDateTime createdAt,
        LocalDateTime lockedAt, String lockReason, String lockedBy) { }
