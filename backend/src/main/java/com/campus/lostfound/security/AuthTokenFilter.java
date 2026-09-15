package com.campus.lostfound.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 令牌认证过滤器，是"当前用户是谁"的唯一入口。
 *
 * <p>每个请求进入 Spring Security 过滤链时执行一次：从 {@code Authorization} 请求头取出
 * {@code Bearer} 令牌，校验通过后把用户主体写入 {@link SecurityContextHolder}，
 * 后续的 {@code @PreAuthorize} 与 {@link CurrentUser} 才有身份可取。
 *
 * <p>职责边界：本类只做<b>认证</b>（判断令牌是否可信），不做<b>授权</b>（判断这个身份能不能访问）。
 * 令牌缺失、非法或过期时一律按"匿名请求"放行，由 Spring Security 的授权规则与
 * {@code SecurityConfig} 中的 EntryPoint 返回 401，避免在过滤器里提前写响应。
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    /** {@code Authorization} 头中 Bearer 令牌的前缀，注意含一个尾随空格。 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService tokenService;

    public AuthTokenFilter(AuthTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 过滤链主流程：尝试认证，然后<b>无条件放行</b>。
     *
     * <p>无论令牌是否有效，请求都会继续往下走 —— 认证失败不等于请求非法，
     * 公开接口（浏览物品、分类）本来就不需要身份。真正的拦截发生在后续的授权规则里。
     * 在这里 return 或写 401 会把公开接口一并挡掉。
     *
     * @param request     当前请求
     * @param response    当前响应，本方法不写入
     * @param filterChain 后续过滤链，必须调用且只调用一次
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        // 已经认证过的上下文不再覆盖，避免过滤器重复执行时把身份改掉
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer 令牌。
     *
     * @param request 当前请求
     * @return 去掉前缀的令牌字符串；请求头缺失、为空串或不是 Bearer 类型时返回 {@code null}
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    /**
     * 解析令牌并把用户主体写入安全上下文。
     *
     * <p>令牌不可信时静默返回 —— 这里不抛异常也不写响应，把"未认证"的事实留给后续授权环节处理。
     *
     * @param token 已去掉 Bearer 前缀的令牌
     */
    private void authenticate(String token) {
        UserPrincipal principal = tokenService.parse(token);
        if (principal == null) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
