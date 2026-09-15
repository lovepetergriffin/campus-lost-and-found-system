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

/**
 * 首次启动时播种演示数据：一个管理员账号与六个默认分类。
 *
 * <p>用 {@link ApplicationRunner} 而非 SQL 脚本，是为了让管理员密码走
 * {@link PasswordEncoder} 生成 —— BCrypt 的盐是随机的，写死在 {@code schema.sql} 里
 * 每次部署都会得到同一份摘要，反而失去加盐的意义。
 *
 * <p><b>幂等</b>：两类数据都先判断"是否已存在"，因此重复启动不会产生重复记录，
 * 也不会覆盖用户后来改过的密码或分类。
 *
 * <p>注意本类只适用于本地开发与演示。生产环境应删除管理员种子，
 * 改用一次性初始化流程，否则默认口令会成为公开的后门。
 */
@Component
public class SeedDataConfig implements ApplicationRunner {

    /** 演示管理员账号，配合 README 中公开的口令 {@code admin123} 使用。 */
    private static final String DEMO_ADMIN_USERNAME = "admin";

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    public SeedDataConfig(UserMapper userMapper, CategoryMapper categoryMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 应用启动完成后的播种入口。
     *
     * <p>两步之间没有依赖关系：管理员绑定的是角色而非分类，分类也不引用用户，
     * 因此谁先执行都不影响结果。拆成两个方法是为了让各自的幂等判断独立可读，
     * 也便于将来单独关闭其中一项（例如生产环境只播种分类）。
     *
     * @param args 启动参数，本方法不使用
     */
    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedCategories();
    }

    /**
     * 播种管理员账号；已存在同名账号时跳过，不覆盖已有密码。
     */
    private void seedAdmin() {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, DEMO_ADMIN_USERNAME)) > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(DEMO_ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setContact("系统管理员");
        admin.setCreatedAt(LocalDateTime.now());
        userMapper.insert(admin);
    }

    /**
     * 播种默认分类；表中已有任意分类时整体跳过。
     *
     * <p>{@code sortOrder} 按 10 递增而非 1，给后续插入的分类留出排序空隙，
     * 调整顺序时不必整体重排。
     */
    private void seedCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        List<String> names = List.of("证件卡类", "电子产品", "书籍资料", "衣物饰品", "生活用品", "其他");
        for (int i = 0; i < names.size(); i++) {
            Category category = new Category();
            category.setName(names.get(i));
            category.setSortOrder((i + 1) * 10);
            categoryMapper.insert(category);
        }
    }
}
