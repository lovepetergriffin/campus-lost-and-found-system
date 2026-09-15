package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Item;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 构建公开物品列表的查询条件；仅负责筛选与排序，不执行数据库查询。
 */
@Component
public class ItemFilterBuilder {

    /**
     * 为一次查询创建独立条件对象，始终限定为已公开物品。
     * 文本参数为空或全空白时忽略对应筛选；各组条件之间使用 AND 连接。
     *
     * @param keyword 名称或描述中的关键词，查询前去除首尾空白
     * @param type 物品类型，非空白时沿用大写转换，不额外去除首尾空白
     * @param categoryId 分类编号，null 表示不限分类
     * @param location 地点片段，查询前去除首尾空白
     * @param startDate 起始日期，包含当天零点；null 表示无下界
     * @param endDate 结束日期，包含当天全部时间；null 表示无上界
     * @return 按发布时间倒序排列的查询条件
     */
    public LambdaQueryWrapper<Item> build(String keyword, String type, Long categoryId, String location,
                                          LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Item> query = new LambdaQueryWrapper<Item>().eq(Item::getStatus, "PUBLISHED");
        applyKeyword(query, keyword);
        applyClassification(query, type, categoryId);
        applyLocation(query, location);
        applyDateRange(query, startDate, endDate);
        return query.orderByDesc(Item::getCreatedAt);
    }

    /**
     * 将名称与描述的 OR 条件放在同一括号内。
     * 括号保证描述命中时仍须满足公开状态及其他筛选条件。
     */
    private void applyKeyword(LambdaQueryWrapper<Item> query, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like(Item::getName, keyword.trim())
                    .or().like(Item::getDescription, keyword.trim()));
        }
    }

    /**
     * 分别追加类型和分类的精确匹配条件，允许只提供其中一项。
     */
    private void applyClassification(LambdaQueryWrapper<Item> query, String type, Long categoryId) {
        if (type != null && !type.isBlank()) query.eq(Item::getType, type.toUpperCase());
        if (categoryId != null) query.eq(Item::getCategoryId, categoryId);
    }

    /**
     * 地点使用去除首尾空白后的片段进行模糊匹配。
     */
    private void applyLocation(LambdaQueryWrapper<Item> query, String location) {
        if (location != null && !location.isBlank()) query.like(Item::getLocation, location.trim());
    }

    /**
     * 使用发生时间筛选，区间为 [起始日零点, 结束日次日零点)。
     * 采用次日零点的严格小于条件，避免遗漏结束日内带小数秒的记录。
     */
    private void applyDateRange(LambdaQueryWrapper<Item> query, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) query.ge(Item::getEventTime, startDate.atStartOfDay());
        if (endDate != null) query.lt(Item::getEventTime, endDate.plusDays(1).atStartOfDay());
    }
}
