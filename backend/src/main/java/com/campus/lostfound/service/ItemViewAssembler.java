package com.campus.lostfound.service;

import com.campus.lostfound.domain.Category;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class ItemViewAssembler {
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public ItemViewAssembler(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public ItemDtos.View toView(Item item) {
        Category category = categoryMapper.selectById(item.getCategoryId());
        User publisher = userMapper.selectById(item.getUserId());
        return new ItemDtos.View(item.getId(), item.getName(), item.getType(), item.getCategoryId(),
                category == null ? "未知分类" : category.getName(), item.getLocation(), item.getEventTime(),
                item.getDescription(), item.getImageUrl(), item.getContact(), item.getStatus(), item.getUserId(),
                publisher == null ? "未知用户" : publisher.getUsername(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
