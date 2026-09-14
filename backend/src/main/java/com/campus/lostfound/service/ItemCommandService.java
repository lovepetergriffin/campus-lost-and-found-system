package com.campus.lostfound.service;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ItemCommandService {
    private final ItemMapper itemMapper;
    private final CategoryService categoryService;
    private final ItemQueryService itemQueryService;
    private final ItemViewAssembler assembler;
    private final ItemAccessPolicy accessPolicy;

    public ItemCommandService(ItemMapper itemMapper, CategoryService categoryService,
                              ItemQueryService itemQueryService, ItemViewAssembler assembler,
                              ItemAccessPolicy accessPolicy) {
        this.itemMapper = itemMapper;
        this.categoryService = categoryService;
        this.itemQueryService = itemQueryService;
        this.assembler = assembler;
        this.accessPolicy = accessPolicy;
    }

    public ItemDtos.View create(ItemDtos.SaveRequest request) {
        categoryService.require(request.categoryId());
        Item item = new Item();
        copyRequest(request, item);
        item.setUserId(CurrentUser.get().id());
        item.setStatus("PENDING");
        touch(item);
        item.setCreatedAt(item.getUpdatedAt());
        itemMapper.insert(item);
        return assembler.toView(item);
    }

    public ItemDtos.View update(Long id, ItemDtos.SaveRequest request) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus()) || "CLOSED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领或已关闭的信息不能修改");
        }
        categoryService.require(request.categoryId());
        copyRequest(request, item);
        item.setStatus("PENDING");
        touch(item);
        itemMapper.updateById(item);
        return assembler.toView(item);
    }

    public void delete(Long id) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领的信息不能删除");
        }
        itemMapper.deleteById(id);
    }

    public void close(Long id) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领的信息不能关闭");
        }
        item.setStatus("CLOSED");
        touch(item);
        itemMapper.updateById(item);
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

    private void touch(Item item) {
        item.setUpdatedAt(LocalDateTime.now());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
