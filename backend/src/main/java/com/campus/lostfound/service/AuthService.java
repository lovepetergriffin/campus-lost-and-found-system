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

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getRole());
        return new AuthDtos.LoginResponse(tokenService.create(principal), toView(user));
    }

    public AuthDtos.UserView me() {
        User user = userMapper.selectById(CurrentUser.get().id());
        if (user == null) throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户不存在");
        return toView(user);
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public static AuthDtos.UserView toView(User user) {
        return new AuthDtos.UserView(user.getId(), user.getUsername(), user.getRole(), user.getContact());
    }
}
