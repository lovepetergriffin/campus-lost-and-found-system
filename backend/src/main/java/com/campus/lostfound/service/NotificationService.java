package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Notification;
import com.campus.lostfound.mapper.NotificationMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知：创建通知、查询本人的通知、标记已读。
 *
 * <p>通知是<b>单向、只写不删</b>的：业务动作发生时由各服务调用 {@link #create} 投递，
 * 收件人只能读和标记已读，没有删除接口 —— 审核结果这类消息属于操作留痕，不应被随手清掉。
 *
 * <p>除 {@link #create} 外，所有查询都以 {@link CurrentUser} 为准过滤收件人，
 * <b>不接受调用方传入用户 id</b>，从签名上杜绝"查看/操作他人通知"的越权。
 */
@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 投递一条通知，供其它业务服务（审核、认领等）在事务内调用。
     *
     * <p>这是一个<b>内部协作接口</b>：收件人由调用方指定，不走当前登录用户校验。
     * 调用方必须自己保证收件人正确 —— 这里允许越权写入，是因为业务动作的接收者
     * 本来就是另一个人（如审核通知发给发布者）。
     *
     * @param receiverId 收件人主键
     * @param type       通知类型，取值如 {@code REVIEW} / {@code CLAIM}，供前端选图标与跳转
     * @param content    通知正文
     */
    public void create(Long receiverId, String type, String content) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 查询当前用户收到的全部通知，最新的排在最前。
     *
     * @return 通知列表，无数据时为空列表
     */
    public List<Notification> mine() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, CurrentUser.get().id())
                .orderByDesc(Notification::getCreatedAt));
    }

    /**
     * 统计当前用户的未读通知数，供前端顶栏红点使用。
     *
     * <p>用 {@code selectCount} 让数据库直接返回计数，而不是把未读列表拉回内存再 {@code size()}。
     *
     * @return 未读条数，无未读时为 0
     */
    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, CurrentUser.get().id())
                .eq(Notification::getIsRead, false));
    }

    /**
     * 把一条通知标记为已读。
     *
     * @param id 通知主键
     * @throws BusinessException 通知不存在时抛出 404；通知不属于当前用户时抛出 403
     */
    public void markRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) throw BusinessException.notFound("通知不存在");
        if (!notification.getReceiverId().equals(CurrentUser.get().id())) {
            throw BusinessException.forbidden("不能操作他人的通知");
        }
        notification.setIsRead(true);
        notificationMapper.updateById(notification);
    }

    /**
     * 把当前用户的全部未读通知一次性标记为已读。
     *
     * <p>只查未读的再逐条更新，而不是无条件把本人所有通知刷成已读 ——
     * 后者虽然结果等价，但会对已经读过的记录产生无谓的写放大。
     */
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
