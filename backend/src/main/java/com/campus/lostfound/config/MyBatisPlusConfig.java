package com.campus.lostfound.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 的插件装配。
 *
 * <p>只注册了一个分页拦截器：MyBatis-Plus 的 {@code Page} 对象本身不会改写 SQL，
 * 真正把查询自动加上 {@code LIMIT} 与 {@code COUNT} 的是这个拦截器。
 * 漏掉它的话，分页查询会静默地一次拉回全表 —— 数据量小时看不出来，上线后才暴露。
 *
 * <p>方言固定为 {@link DbType#MYSQL}：默认运行库是 H2（见 {@code application.yml}），
 * 但分页语法与 MySQL 兼容，因此两种库共用同一份配置，不需要按 profile 切换。
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册分页拦截器。
     *
     * @return 已装配分页能力的 MyBatis-Plus 拦截器链
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
