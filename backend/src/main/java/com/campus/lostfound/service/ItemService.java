package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.domain.Category;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ItemService {
    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;

    public ItemService(ItemMapper itemMapper, CategoryMapper categoryMapper, UserMapper userMapper,
                       CategoryService categoryService) {
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.categoryService = categoryService;
    }

    public PageResult<ItemDtos.View> publicList(String keyword, String type, Long categoryId, String location,
                                                LocalDate startDate, LocalDate endDate, long page, long size) {
        LambdaQueryWrapper<Item> query = new LambdaQueryWrapper<Item>().eq(Item::getStatus, "PUBLISHED");
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like(Item::getName, keyword.trim()).or().like(Item::getDescription, keyword.trim()));
        }
        if (type != null && !type.isBlank()) query.eq(Item::getType, type.toUpperCase());
        if (categoryId != null) query.eq(Item::getCategoryId, categoryId);
        if (location != null && !location.isBlank()) query.like(Item::getLocation, location.trim());
        if (startDate != null) query.ge(Item::getEventTime, startDate.atStartOfDay());
        if (endDate != null) query.lt(Item::getEventTime, endDate.plusDays(1).atStartOfDay());
        query.orderByDesc(Item::getCreatedAt);
        Page<Item> result = itemMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), query);
        return new PageResult<>(result.getRecords().stream().map(this::toView).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ItemDtos.View detail(Long id) {
        Item item = require(id);
        if (!"PUBLISHED".equals(item.getStatus()) && !canManage(item)) {
            throw BusinessException.notFound("物品信息不存在或尚未公开");
        }
        return toView(item);
    }

    public ItemDtos.View create(ItemDtos.SaveRequest request) {
        categoryService.require(request.categoryId());
        Item item = new Item();
        copyRequest(request, item);
        item.setUserId(CurrentUser.get().id());
        item.setStatus("PENDING");
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        return toView(item);
    }

    public ItemDtos.View update(Long id, ItemDtos.SaveRequest request) {
        Item item = require(id);
        assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus()) || "CLOSED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领或已关闭的信息不能修改");
        }
        categoryService.require(request.categoryId());
        copyRequest(request, item);
        item.setStatus("PENDING");
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return toView(item);
    }

    public void delete(Long id) {
        Item item = require(id);
        assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) throw BusinessException.badRequest("已认领的信息不能删除");
        itemMapper.deleteById(id);
    }

    public List<ItemDtos.View> mine() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, CurrentUser.get().id()).orderByDesc(Item::getCreatedAt))
                .stream().map(this::toView).toList();
    }

    public void close(Long id) {
        Item item = require(id);
        assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) throw BusinessException.badRequest("已认领的信息不能关闭");
        item.setStatus("CLOSED");
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    public Item require(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("物品信息不存在");
        return item;
    }

    public ItemDtos.View toView(Item item) {
        Category category = categoryMapper.selectById(item.getCategoryId());
        User publisher = userMapper.selectById(item.getUserId());
        return new ItemDtos.View(item.getId(), item.getName(), item.getType(), item.getCategoryId(),
                category == null ? "未知分类" : category.getName(), item.getLocation(), item.getEventTime(),
                item.getDescription(), item.getImageUrl(), item.getContact(), item.getStatus(), item.getUserId(),
                publisher == null ? "未知用户" : publisher.getUsername(), item.getCreatedAt(), item.getUpdatedAt());
    }

    private void copyRequest(ItemDtos.SaveRequest request, Item item) {
        item.setName(request.name().trim());
        item.setType(request.type().toUpperCase());
        item.setCategoryId(request.categoryId());
        item.setLocation(request.location().trim());
        item.setEventTime(request.eventTime());
        item.setDescription(request.description().trim());
        item.setImageUrl(blankToNull(request.imageUrl()));
        item.setContact(blankToNull(request.contact()));
    }

    private void assertCanEdit(Item item) {
        if (!canManage(item)) throw BusinessException.forbidden("只能管理自己发布的信息");
    }

    private boolean canManage(Item item) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) return false;
        return item.getUserId().equals(principal.id()) || "ADMIN".equals(principal.role());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
