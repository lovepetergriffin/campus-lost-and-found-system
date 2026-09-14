package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ItemQueryService {
    private final ItemMapper itemMapper;
    private final ItemViewAssembler assembler;
    private final ItemFilterBuilder filterBuilder;
    private final ItemAccessPolicy accessPolicy;

    public ItemQueryService(ItemMapper itemMapper, ItemViewAssembler assembler,
                            ItemFilterBuilder filterBuilder, ItemAccessPolicy accessPolicy) {
        this.itemMapper = itemMapper;
        this.assembler = assembler;
        this.filterBuilder = filterBuilder;
        this.accessPolicy = accessPolicy;
    }

    public PageResult<ItemDtos.View> publicList(String keyword, String type, Long categoryId, String location,
                                                LocalDate startDate, LocalDate endDate, long page, long size) {
        LambdaQueryWrapper<Item> query = filterBuilder.build(
                keyword, type, categoryId, location, startDate, endDate);

        Page<Item> result = itemMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), query);
        return new PageResult<>(result.getRecords().stream().map(assembler::toView).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ItemDtos.View detail(Long id) {
        Item item = require(id);
        if (!"PUBLISHED".equals(item.getStatus()) && !accessPolicy.canManage(item)) {
            throw BusinessException.notFound("物品信息不存在或尚未公开");
        }
        return assembler.toView(item);
    }

    public List<ItemDtos.View> mine() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, CurrentUser.get().id()).orderByDesc(Item::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    public Item require(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("物品信息不存在");
        return item;
    }

}
