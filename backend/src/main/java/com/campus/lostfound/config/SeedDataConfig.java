package com.campus.lostfound.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Category;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SeedDataConfig implements ApplicationRunner {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    public SeedDataConfig(UserMapper userMapper, CategoryMapper categoryMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setContact("系统管理员");
            admin.setCreatedAt(LocalDateTime.now());
            userMapper.insert(admin);
        }
        if (categoryMapper.selectCount(null) == 0) {
            List<String> names = List.of("证件卡类", "电子产品", "书籍资料", "衣物饰品", "生活用品", "其他");
            for (int i = 0; i < names.size(); i++) {
                Category category = new Category();
                category.setName(names.get(i));
                category.setSortOrder((i + 1) * 10);
                categoryMapper.insert(category);
            }
        }
    }
}
