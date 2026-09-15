package com.campus.lostfound.service;

import com.campus.lostfound.domain.Category;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 将物品实体转换为前端使用的只读视图，补充分类和发布者名称。
 */
@Component
public class ItemViewAssembler {
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    /**
     * 注入用于读取分类和发布者展示名称的映射器。
     */
    public ItemViewAssembler(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    /**
     * 组装物品视图；关联分类或用户不存在时使用“未知”名称。
     * 此方法不执行权限检查，调用方必须先确认物品可以对当前访问者展示。
     *
     * @param item 已加载且允许展示的物品实体
     * @return 包含分类名称和发布者名称的物品视图
     */
    public ItemDtos.View toView(Item item) {
        Category category = categoryMapper.selectById(item.getCategoryId());
        User publisher = userMapper.selectById(item.getUserId());
        return new ItemDtos.View(item.getId(), item.getName(), item.getType(), item.getCategoryId(),
                category == null ? "未知分类" : category.getName(), item.getLocation(), item.getEventTime(),
                item.getDescription(), item.getImageUrl(), item.getContact(), item.getStatus(), item.getUserId(),
                publisher == null ? "未知用户" : publisher.getUsername(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
