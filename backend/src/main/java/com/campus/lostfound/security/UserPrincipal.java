package com.campus.lostfound.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户的身份快照，同时实现 {@link UserDetails} 以便接入 Spring Security。
 *
 * <p>数据来源是令牌载荷，不查库 —— 因此它只反映<b>签发令牌那一刻</b>的用户信息。
 * 如果用户的角色在令牌有效期内被改动，要等令牌过期重新登录才会生效；
 * 需要即时生效的场景应改查数据库。这也是无状态令牌方案的固有取舍。
 *
 * <p>{@link #getAuthorities()} 会把 {@code role} 加上 {@code ROLE_} 前缀，
 * 与 {@code SecurityConfig} 里的 {@code hasRole('ADMIN')} 对应；
 * 前端传入的 {@code ADMIN} 在这里变成 {@code ROLE_ADMIN}。
 *
 * @param id       用户主键
 * @param username 登录名
 * @param role     角色标识，取值 {@code USER} / {@code ADMIN}
 */
public record UserPrincipal(Long id, String username, String role) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * 令牌认证场景下不存在表单密码，恒返回空串。
     *
     * <p>认证已由 {@link AuthTokenService} 的签名校验完成，Spring Security 不会再拿这个值去比对；
     * 返回空串而非 {@code null} 是为了满足接口约定，避免下游校验器抛空指针。
     */
    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账号未过期，恒为 {@code true}。
     *
     * <p>下面四个 {@code is*} 方法都是 Spring Security 为"账号生命周期"预留的扩展点。
     * 本系统不建模过期时间，因此不查库、直接放行 —— 但<b>它们不是可以随手删掉的空实现</b>：
     * 若将来引入账号有效期，改动点就在这里。
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号未被锁定，恒为 {@code true}。
     *
     * <p>引入"管理员封禁用户"功能时，应改为读取用户表上的锁定标记，
     * 并把 {@link #isEnabled()} 一并接上 —— 只改一处会让封禁形同虚设。
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** 凭据未过期，恒为 {@code true}；本系统不强制密码轮换。 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 账号可用，恒为 {@code true}；停用账号的能力见 {@link #isAccountNonLocked()}。 */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
