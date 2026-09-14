package com.campus.lostfound.service;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.mapper.ItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ItemLifecycleService {
    private final ItemMapper itemMapper;
    private final ItemQueryService itemQueryService;

    public ItemLifecycleService(ItemMapper itemMapper, ItemQueryService itemQueryService) {
        this.itemMapper = itemMapper;
        this.itemQueryService = itemQueryService;
    }

    public Item applyReview(Long itemId, String result) {
        Item item = itemQueryService.require(itemId);
        if (!"PENDING".equals(item.getStatus())) {
            throw BusinessException.badRequest("该信息不处于待审核状态");
        }
        item.setStatus("APPROVED".equals(result) ? "PUBLISHED" : "REJECTED");
        save(item);
        return item;
    }

    public void markClaimed(Long itemId) {
        Item item = itemQueryService.require(itemId);
        if (!"PUBLISHED".equals(item.getStatus())) {
            throw BusinessException.badRequest("该物品当前不可处理认领");
        }
        item.setStatus("CLAIMED");
        save(item);
    }

    private void save(Item item) {
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
    }
}
