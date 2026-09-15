package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.domain.Notification;
import com.campus.lostfound.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内通知的 HTTP 入口，统一挂在 {@code /api/notifications} 下。
 *
 * <p>所有端点都只作用于<b>当前登录用户自己的</b>通知：收件人过滤在服务层用当前身份完成，
 * 路径参数里也没有"用户 id"这一项，客户端无法通过改参数去看别人的通知。
 *
 * <p>已知问题：列表接口直接返回领域实体 {@code Notification}，把 {@code receiverId}
 * 等内部字段一并暴露了出去。其余模块（物品、认领、认证）都走了 DTO，唯独这里没有，
 * 属重构遗漏；补齐 DTO 会改变响应字段，故本期未动。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 当前用户的通知列表，最新的在最前。
     *
     * @return 通知列表，无数据时为空列表
     */
    @GetMapping
    public ApiResponse<List<Notification>> list() {
        return ApiResponse.ok(notificationService.mine());
    }

    /**
     * 未读通知条数，供顶栏红点轮询。
     *
     * @return 形如 {@code {"count": 3}} 的映射
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    /**
     * 把一条通知标记为已读。
     *
     * @param id 通知主键
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.ok();
    }

    /**
     * 把当前用户的全部未读通知标记为已读。
     *
     * @return 空响应体
     */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok();
    }
}
