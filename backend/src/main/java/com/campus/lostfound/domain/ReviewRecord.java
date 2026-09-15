package com.campus.lostfound.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品审核记录实体，映射数据库表 {@code review_record}。
 *
 * <p>这是<b>审计流水</b>：每审核一次就追加一条，与物品当前状态无关。
 * {@code Item.status} 只告诉你"现在是什么状态"，本表回答"它是怎么变成这个状态的、谁经手的"。
 * 因此记录只增不改不删。
 *
 * <p>同一件物品可能有多条记录（例如先被驳回、修改后重新提交再被通过），
 * 需要完整轨迹时应按 {@code itemId} 查询并按 {@code reviewTime} 排序。
 */
@Data
@TableName("review_record")
public class ReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被审核物品的主键，指向 {@code item.id}。 */
    private Long itemId;

    /**
     * 审核人主键，指向 {@code sys_user.id}。
     *
     * <p>写入时取自服务端的当前登录身份，不接受客户端传入，保证审计链可信。
     */
    private Long reviewerId;

    /** 审核结论，取值 {@code APPROVED} / {@code REJECTED}。 */
    private String result;

    /** 审核意见，可空；空白输入会在写入前被归一为 {@code null}。 */
    private String comment;

    /** 审核发生的时刻，由服务端取当前时间。 */
    private LocalDateTime reviewTime;
}
