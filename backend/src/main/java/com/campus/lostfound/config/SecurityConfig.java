package com.campus.lostfound.config;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.security.AuthTokenFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 的总装配：认证方式、放行规则、异常响应与跨域策略都在这里定。
 *
 * <p>三条主线：
 * <ol>
 *   <li><b>无状态令牌认证</b> —— 关闭 session，认证完全由 {@link AuthTokenFilter} 承担，
 *       服务端不保存登录态，因此可以水平扩容；代价是令牌一旦签发就无法单方面作废。</li>
 *   <li><b>放行规则</b> —— 白名单只有注册、登录、分类浏览、物品浏览与 H2 控制台，
 *       其余一律要求已认证。需要管理员身份的端点由 {@code @PreAuthorize} 在方法层再加一道。</li>
 *   <li><b>错误响应统一为 JSON</b> —— 未认证 401、无权限 403 都返回与业务接口同构的
 *       {@link ApiResponse} 结构，前端可以只写一套解析逻辑，不必区分"是网关拦的还是业务抛的"。</li>
 * </ol>
 *
 * <p>关闭 CSRF 的前提是认证凭据放在 {@code Authorization} 头而非 Cookie：
 * 浏览器不会自动携带自定义头，因此不存在跨站伪造请求的通道。
 * <b>若将来把令牌改存 Cookie，必须同时把 CSRF 防护打开。</b>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 定义安全过滤链。
     *
     * @param http         Spring Security 的构建入口
     * @param tokenFilter  自定义令牌过滤器，插在用户名密码过滤器之前
     * @param objectMapper 用于把错误响应序列化为 JSON
     * @return 装配完成的过滤链
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenFilter tokenFilter,
                                            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/*").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                // H2 控制台用 iframe 渲染，同源放行否则被 X-Frame-Options 挡掉
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getWriter(), ApiResponse.error(401, "请先登录"));
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getWriter(), ApiResponse.error(403, "没有操作权限"));
                        }))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 密码哈希器。
     *
     * <p>选 BCrypt 而非 MD5/SHA：它自带随机盐（同一密码两次哈希结果不同，彩虹表失效），
     * 且计算成本可调，能随硬件发展提高破解代价。
     *
     * @return BCrypt 实现的密码编码器
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域策略，供前端开发服务器（Vite 默认 5173 端口）直连后端。
     *
     * <p>白名单只列本机开发地址。部署到真实域名时<b>必须</b>把生产域名加进来 ——
     * 也正因如此这里不能图省事写成通配符 {@code *}：一旦开启
     * {@code allowCredentials}，通配符来源会被浏览器直接拒绝，且会放开任意站点的跨域读取。
     *
     * @return 基于路径注册的跨域配置源
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
