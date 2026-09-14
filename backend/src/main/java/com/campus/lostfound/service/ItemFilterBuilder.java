package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Item;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ItemFilterBuilder {

    public LambdaQueryWrapper<Item> build(String keyword, String type, Long categoryId, String location,
                                          LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Item> query = new LambdaQueryWrapper<Item>().eq(Item::getStatus, "PUBLISHED");
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like(Item::getName, keyword.trim())
                    .or().like(Item::getDescription, keyword.trim()));
        }
        if (type != null && !type.isBlank()) query.eq(Item::getType, type.toUpperCase());
        if (categoryId != null) query.eq(Item::getCategoryId, categoryId);
        if (location != null && !location.isBlank()) query.like(Item::getLocation, location.trim());
        if (startDate != null) query.ge(Item::getEventTime, startDate.atStartOfDay());
        if (endDate != null) query.lt(Item::getEventTime, endDate.plusDays(1).atStartOfDay());
        return query.orderByDesc(Item::getCreatedAt);
    }
}
