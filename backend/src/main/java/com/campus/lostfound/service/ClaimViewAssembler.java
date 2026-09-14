package com.campus.lostfound.service;

import com.campus.lostfound.domain.Claim;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class ClaimViewAssembler {
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public ClaimViewAssembler(ItemMapper itemMapper, UserMapper userMapper) {
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
    }

    public ClaimDtos.View toView(Claim claim) {
        Item item = itemMapper.selectById(claim.getItemId());
        User applicant = userMapper.selectById(claim.getApplicantId());
        return new ClaimDtos.View(claim.getId(), claim.getItemId(), item == null ? "未知物品" : item.getName(),
                claim.getApplicantId(), applicant == null ? "未知用户" : applicant.getUsername(),
                claim.getDescription(), claim.getProof(), claim.getStatus(), claim.getCreatedAt(), claim.getProcessedAt());
    }
}
