package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;
    private final UserMapper userMapper;

    public StatisticsService(ItemMapper itemMapper, ClaimMapper claimMapper, UserMapper userMapper) {
        this.itemMapper = itemMapper;
        this.claimMapper = claimMapper;
        this.userMapper = userMapper;
    }

    public Map<String, Object> statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        long claimedCount = countItemsByStatus("CLAIMED");
        long publishedOrClaimed = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .in(Item::getStatus, List.of("PUBLISHED", "CLAIMED")));

        data.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));
        data.put("itemCount", itemMapper.selectCount(null));
        data.put("publishedCount", countItemsByStatus("PUBLISHED"));
        data.put("pendingCount", countItemsByStatus("PENDING"));
        data.put("claimedCount", claimedCount);
        data.put("claimCount", claimMapper.selectCount(null));
        data.put("claimRate", percentage(claimedCount, publishedOrClaimed));
        data.put("lostCount", countItemsByType("LOST"));
        data.put("foundCount", countItemsByType("FOUND"));
        return data;
    }

    private long countItemsByStatus(String status) {
        return itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, status));
    }

    private long countItemsByType(String type) {
        return itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getType, type));
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 10000.0 / total) / 100.0;
    }
}
