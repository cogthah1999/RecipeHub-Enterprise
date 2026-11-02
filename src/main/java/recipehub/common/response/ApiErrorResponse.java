package recipehub.common.response;

public record ApiErrorResponse(
        int status,
        String message
) {
}