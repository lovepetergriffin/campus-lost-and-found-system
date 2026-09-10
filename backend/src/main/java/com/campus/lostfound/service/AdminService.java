package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Claim;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.ReviewRecord;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.ReviewRecordMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;
    private final UserMapper userMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ItemService itemService;
    private final NotificationService notificationService;

    public AdminService(ItemMapper itemMapper, ClaimMapper claimMapper, UserMapper userMapper,
                        ReviewRecordMapper reviewRecordMapper, ItemService itemService,
                        NotificationService notificationService) {
        this.itemMapper = itemMapper;
        this.claimMapper = claimMapper;
        this.userMapper = userMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.itemService = itemService;
        this.notificationService = notificationService;
    }

    public List<ItemDtos.View> pendingItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getStatus, "PENDING").orderByAsc(Item::getCreatedAt))
                .stream().map(itemService::toView).toList();
    }

    @Transactional
    public ItemDtos.View review(Long itemId, String result, String comment) {
        Item item = itemService.require(itemId);
        if (!"PENDING".equals(item.getStatus())) throw BusinessException.badRequest("该信息不处于待审核状态");
        item.setStatus("APPROVED".equals(result) ? "PUBLISHED" : "REJECTED");
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);

        ReviewRecord record = new ReviewRecord();
        record.setItemId(itemId);
        record.setReviewerId(CurrentUser.get().id());
        record.setResult(result);
        record.setComment(comment == null ? null : comment.trim());
        record.setReviewTime(LocalDateTime.now());
        reviewRecordMapper.insert(record);

        String resultText = "APPROVED".equals(result) ? "审核通过" : "审核未通过";
        String suffix = comment == null || comment.isBlank() ? "" : "：" + comment.trim();
        notificationService.create(item.getUserId(), "REVIEW", "“" + item.getName() + "”" + resultText + suffix);
        return itemService.toView(item);
    }

    public Map<String, Object> statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));
        data.put("itemCount", itemMapper.selectCount(null));
        data.put("publishedCount", itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, "PUBLISHED")));
        data.put("pendingCount", itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, "PENDING")));
        data.put("claimedCount", itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, "CLAIMED")));
        data.put("claimCount", claimMapper.selectCount(null));
        long publishedOrClaimed = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .in(Item::getStatus, List.of("PUBLISHED", "CLAIMED")));
        long claimed = (long) data.get("claimedCount");
        data.put("claimRate", publishedOrClaimed == 0 ? 0 : Math.round(claimed * 10000.0 / publishedOrClaimed) / 100.0);
        data.put("lostCount", itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getType, "LOST")));
        data.put("foundCount", itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getType, "FOUND")));
        return data;
    }
}
