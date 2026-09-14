package com.campus.lostfound.service;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ItemAccessPolicy {

    public void assertCanEdit(Item item) {
        if (!canManage(item)) {
            throw BusinessException.forbidden("只能管理自己发布的信息");
        }
    }

    public boolean canManage(Item item) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return item.getUserId().equals(principal.id()) || "ADMIN".equals(principal.role());
    }
}
