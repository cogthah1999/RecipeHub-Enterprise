package recipehub.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockUserRequest(
        @NotBlank(message = "Vui lòng nhập lý do khóa tài khoản")
        @Size(max = 250, message = "Lý do khóa tối đa 250 ký tự")
        String reason) { }
