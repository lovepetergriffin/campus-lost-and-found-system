package com.campus.lostfound.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，映射数据库表 {@code notification}。
 *
 * <p>一个用户收到的所有通知都存在这张表里，由 {@code receiverId} 区分收件人。
 * 通知由业务动作触发写入（审核结果、认领进展等），收件人只读与标记已读，不提供删除。
 *
 * <p>本类目前被直接用作 API 响应体（见 {@code NotificationController}），
 * 因此 {@code receiverId} 会一并暴露 —— 这是已知的重构遗漏，补 DTO 时需同步改前端。
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收件人主键，指向 {@code sys_user.id}；查询时恒按当前登录用户过滤。 */
    private Long receiverId;

    /** 通知类型，取值如 {@code REVIEW}（审核结果）/ {@code CLAIM}（认领进展），供前端选图标与跳转目标。 */
    private String type;

    /** 通知正文，由各业务服务在投递时拼好，读取方不做二次解析。 */
    private String content;

    /**
     * 是否已读。
     *
     * <p>字段名以 {@code is} 开头，Lombok 生成的读写方法为 {@code getIsRead} / {@code setIsRead}
     * 而非 {@code isRead}，调用时注意别写错。
     */
    private Boolean isRead;

    private LocalDateTime createdAt;
}
