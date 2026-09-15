package com.campus.lostfound.service;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 集中定义物品管理权限：发布者本人或管理员可管理物品。
 */
@Component
public class ItemAccessPolicy {

    /**
     * 在写操作前检查管理权限，不负责检查物品当前状态。
     *
     * @param item 已加载的物品实体
     * @throws BusinessException 当前访问者不具备管理权限
     */
    public void assertCanEdit(Item item) {
        if (!canManage(item)) {
            throw BusinessException.forbidden("只能管理自己发布的信息");
        }
    }

    /**
     * 检查当前认证主体是否为发布者本人或管理员。
     * 未认证或主体不是系统用户类型时返回 false。
     *
     * @param item 已加载的物品实体
     * @return 当前访问者是否具有管理权限
     */
    public boolean canManage(Item item) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return item.getUserId().equals(principal.id()) || "ADMIN".equals(principal.role());
    }
}
