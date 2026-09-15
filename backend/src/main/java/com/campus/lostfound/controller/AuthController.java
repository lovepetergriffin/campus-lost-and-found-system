package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.dto.AuthDtos;
import com.campus.lostfound.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关的 HTTP 入口，统一挂在 {@code /api/auth} 下。
 *
 * <p>本类不含任何业务判断，只做"收参数 → 调服务 → 包响应"三件事；
 * 注册与登录两个端点已在 {@code SecurityConfig} 中放行，{@code /me} 需要携带令牌。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户。
     *
     * @param request 注册请求体，字段约束见 {@link AuthDtos.RegisterRequest}
     * @return 新用户的只读视图
     */
    @PostMapping("/register")
    public ApiResponse<AuthDtos.UserView> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    /**
     * 登录并获取令牌，前端后续请求需在 {@code Authorization} 头携带 {@code Bearer <token>}。
     *
     * @param request 登录请求体
     * @return 令牌与用户信息
     */
    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 查询当前登录用户，用于前端刷新页面后恢复登录态。
     *
     * @return 当前用户的只读视图
     */
    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me() {
        return ApiResponse.ok(authService.me());
    }
}
