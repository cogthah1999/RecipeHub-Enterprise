package recipehub.auth.dto;

public record AuthResponse(String tokenType, String accessToken, Long userId, String username) { }
