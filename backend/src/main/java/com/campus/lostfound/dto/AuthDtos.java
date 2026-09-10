package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 30, message = "用户名长度应为 3 到 30 个字符")
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 64, message = "密码长度应为 6 到 64 个字符")
            String password,
            @Size(max = 100, message = "联系方式不能超过 100 个字符")
            String contact
    ) {}

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}

    public record UserView(Long id, String username, String role, String contact) {}

    public record LoginResponse(String token, UserView user) {}
}
