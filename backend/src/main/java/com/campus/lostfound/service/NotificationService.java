package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Notification;
import com.campus.lostfound.mapper.NotificationMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public void create(Long receiverId, String type, String content) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    public List<Notification> mine() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, CurrentUser.get().id())
                .orderByDesc(Notification::getCreatedAt));
    }

    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, CurrentUser.get().id())
                .eq(Notification::getIsRead, false));
    }

    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) throw BusinessException.notFound("通知不存在");
        if (!notification.getReceiverId().equals(CurrentUser.get().id())) {
            throw BusinessException.forbidden("不能操作他人的通知");
        }
        notification.setIsRead(true);
        notificationMapper.updateById(notification);
    }

    public void markAllRead() {
        List<Notification> notifications = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, CurrentUser.get().id())
                .eq(Notification::getIsRead, false));
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
    }
}
