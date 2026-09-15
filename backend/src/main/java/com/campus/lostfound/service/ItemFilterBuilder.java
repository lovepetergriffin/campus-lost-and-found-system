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
        applyKeyword(query, keyword);
        applyClassification(query, type, categoryId);
        applyLocation(query, location);
        applyDateRange(query, startDate, endDate);
        return query.orderByDesc(Item::getCreatedAt);
    }

    private void applyKeyword(LambdaQueryWrapper<Item> query, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like(Item::getName, keyword.trim())
                    .or().like(Item::getDescription, keyword.trim()));
        }
    }

    private void applyClassification(LambdaQueryWrapper<Item> query, String type, Long categoryId) {
        if (type != null && !type.isBlank()) query.eq(Item::getType, type.toUpperCase());
        if (categoryId != null) query.eq(Item::getCategoryId, categoryId);
    }

    private void applyLocation(LambdaQueryWrapper<Item> query, String location) {
        if (location != null && !location.isBlank()) query.like(Item::getLocation, location.trim());
    }

    private void applyDateRange(LambdaQueryWrapper<Item> query, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) query.ge(Item::getEventTime, startDate.atStartOfDay());
        if (endDate != null) query.lt(Item::getEventTime, endDate.plusDays(1).atStartOfDay());
    }
}
