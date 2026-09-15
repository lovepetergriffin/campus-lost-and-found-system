package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 认证模块的请求与响应契约。
 *
 * <p>集中定义在同一个容器类里，让"接口收什么、返什么"一眼可见；
 * 请求对象上的 Jakarta Validation 注解由 {@code @Valid} 触发，在进入服务层之前就拦掉非法输入。
 *
 * <p>注意方向性：{@code *Request} 是入参（字段宽），{@code *View} / {@code *Response} 是出参。
 * 出参一律不含密码字段 —— 实体 {@code User} 绝不直接出现在响应里。
 */
public final class AuthDtos {

    /** 工具类，禁止实例化。 */
    private AuthDtos() {
    }

    /**
     * 注册请求。
     *
     * <p>用户名的字符集限制在字母、数字、下划线：它会被用在登录名比对与前端展示上，
     * 限制字符集可以省掉后续对空白、控制字符、同形异义字的处理。
     *
     * @param username 登录名，3–30 字符
     * @param password 明文密码，6–64 字符，落库前由 {@code PasswordEncoder} 做 BCrypt 哈希
     * @param contact  联系方式，可空，最长 100 字符
     */
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

    /**
     * 登录请求。
     *
     * <p>这里只校验"非空"，不校验长度与字符集 —— 登录时输入的是历史密码，
     * 加上注册时的长度限制会把旧账号锁死在门外，而且多一条校验规则就多一条可用于探测的回显。
     *
     * @param username 登录名
     * @param password 明文密码
     */
    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}

    /**
     * 用户信息的对外视图，刻意不含密码摘要字段。
     *
     * @param id       用户主键
     * @param username 登录名
     * @param role     角色标识，取值 {@code USER} / {@code ADMIN}，前端据此决定是否展示管理入口
     * @param contact  联系方式
     */
    public record UserView(Long id, String username, String role, String contact) {}

    /**
     * 登录成功的响应体。
     *
     * @param token 访问令牌，前端应存入本地并在后续请求的 {@code Authorization} 头携带
     * @param user  该令牌对应的用户信息，前端可直接用于渲染，省去一次 {@code /me} 请求
     */
    public record LoginResponse(String token, UserView user) {}
}
