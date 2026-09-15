package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.domain.Notification;

/**
 * {@link Notification} 的数据访问接口。
 *
 * <p>继承 {@code BaseMapper} 即获得按主键增删改查、条件构造器查询与计数等通用能力，
 * MyBatis-Plus 会自动生成实现。通知的查询条件（按收件人、按已读状态）都能用
 * {@code LambdaQueryWrapper} 表达，因此这里无需额外方法。
 */
public interface NotificationMapper extends BaseMapper<Notification> {
}
