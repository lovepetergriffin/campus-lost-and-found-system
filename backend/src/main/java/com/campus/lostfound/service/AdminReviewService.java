package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.ReviewRecord;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.ReviewRecordMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminReviewService {
    private final ItemMapper itemMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ItemLifecycleService itemLifecycleService;
    private final ItemViewAssembler assembler;
    private final NotificationService notificationService;

    public AdminReviewService(ItemMapper itemMapper, ReviewRecordMapper reviewRecordMapper,
                              ItemLifecycleService itemLifecycleService, ItemViewAssembler assembler,
                              NotificationService notificationService) {
        this.itemMapper = itemMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.itemLifecycleService = itemLifecycleService;
        this.assembler = assembler;
        this.notificationService = notificationService;
    }

    public List<ItemDtos.View> pendingItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getStatus, "PENDING").orderByAsc(Item::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    @Transactional
    public ItemDtos.View review(Long itemId, String result, String comment) {
        Item item = itemLifecycleService.applyReview(itemId, result);
        saveReviewRecord(itemId, result, comment);
        notifyPublisher(item, result, comment);
        return assembler.toView(item);
    }

    private void saveReviewRecord(Long itemId, String result, String comment) {
        ReviewRecord record = new ReviewRecord();
        record.setItemId(itemId);
        record.setReviewerId(CurrentUser.get().id());
        record.setResult(result);
        record.setComment(trimToNull(comment));
        record.setReviewTime(LocalDateTime.now());
        reviewRecordMapper.insert(record);
    }

    private void notifyPublisher(Item item, String result, String comment) {
        String resultText = "APPROVED".equals(result) ? "审核通过" : "审核未通过";
        String normalizedComment = trimToNull(comment);
        String suffix = normalizedComment == null ? "" : "：" + normalizedComment;
        notificationService.create(item.getUserId(), "REVIEW",
                "“" + item.getName() + "”" + resultText + suffix);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
