package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.AuthDtos;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.AuthTokenService;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户账号的注册、登录与资料查询。
 *
 * <p>分工上，本类只处理"账号本身的业务规则"（用户名唯一、密码比对、注册默认角色），
 * 令牌的签发交给 {@link AuthTokenService}，密码的哈希与校验交给 {@link PasswordEncoder}。
 * 这样换令牌方案或换哈希算法都不会波及这里的业务判断。
 *
 * <p>密码在库中只以 BCrypt 摘要形式存在，任何出口（{@link AuthDtos.UserView}）都不含密码字段。
 */
@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, AuthTokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * 注册新用户，角色固定为 {@code USER}（管理员账号由 {@code SeedDataConfig} 播种，不开放自助注册）。
     *
     * @param request 注册请求，用户名字段已由 {@code @Valid} 做过格式与长度校验
     * @return 新用户的只读视图
     * @throws BusinessException 用户名已被占用时抛出 400
     */
    public AuthDtos.UserView register(AuthDtos.RegisterRequest request) {
        if (findByUsername(request.username()) != null) {
            throw BusinessException.badRequest("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setContact(request.contact());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return toView(user);
    }

    /**
     * 校验用户名与密码并签发登录令牌。
     *
     * <p>「用户不存在」与「密码错误」返回同一句提示、同一个状态码，避免通过错误信息差异
     * 枚举出系统里有哪些用户名。密码比对走 {@link PasswordEncoder#matches}，
     * 由 BCrypt 自行处理加盐与定长比较。
     *
     * @param request 登录请求
     * @return 新签发的令牌与该用户的只读视图
     * @throws BusinessException 用户名不存在或密码不匹配时抛出 401
     */
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getRole());
        return new AuthDtos.LoginResponse(tokenService.create(principal), toView(user));
    }

    /**
     * 查询当前登录用户的资料，供前端刷新页面后恢复登录态。
     *
     * <p>此处按 id 回查数据库而不是直接用令牌里的快照，是为了让联系方式等资料的修改即时可见。
     *
     * @return 当前用户的只读视图
     * @throws BusinessException 未登录，或令牌合法但用户已被删除时抛出 401
     */
    public AuthDtos.UserView me() {
        User user = userMapper.selectById(CurrentUser.get().id());
        if (user == null) throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户不存在");
        return toView(user);
    }

    /**
     * 按登录名精确查询用户。
     *
     * @return 匹配到的用户；不存在时返回 {@code null}
     */
    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 把实体裁剪为对外视图，丢弃密码摘要等敏感字段。
     *
     * <p>声明为 {@code public static} 是为了让同样需要输出用户信息的相邻服务复用，
     * 不必各自再写一遍字段映射（避免某处漏删敏感字段）。
     */
    public static AuthDtos.UserView toView(User user) {
        return new AuthDtos.UserView(user.getId(), user.getUsername(), user.getRole(), user.getContact());
    }
}
