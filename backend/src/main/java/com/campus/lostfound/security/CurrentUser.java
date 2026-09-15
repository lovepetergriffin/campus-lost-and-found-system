package com.campus.lostfound.security;

import com.campus.lostfound.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从安全上下文中取当前登录用户的静态入口。
 *
 * <p>服务层需要知道"这个操作是谁发起的"时统一走这里，而不是各自去读
 * {@link SecurityContextHolder} —— 后者会让身份来源散落各处，也容易漏掉类型判断。
 *
 * <p>本类不持有状态，也不参与 Spring 依赖注入，纯粹是上下文读取的语法糖。
 */
public final class CurrentUser {

    /** 工具类，禁止实例化。 */
    private CurrentUser() {
    }

    /**
     * 取当前登录用户，取不到即视为未认证。
     *
     * @return 当前用户主体
     * @throws BusinessException 未登录，或上下文中的主体不是 {@link UserPrincipal} 时抛出 401
     */
    public static UserPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return principal;
    }
}
