package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Claim;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimDecisionService {
    private final ClaimMapper claimMapper;
    private final ItemQueryService itemQueryService;
    private final ItemLifecycleService itemLifecycleService;
    private final ClaimViewAssembler assembler;
    private final NotificationService notificationService;

    public ClaimDecisionService(ClaimMapper claimMapper, ItemQueryService itemQueryService,
                                ItemLifecycleService itemLifecycleService, ClaimViewAssembler assembler,
                                NotificationService notificationService) {
        this.claimMapper = claimMapper;
        this.itemQueryService = itemQueryService;
        this.itemLifecycleService = itemLifecycleService;
        this.assembler = assembler;
        this.notificationService = notificationService;
    }

    @Transactional
    public ClaimDtos.View decide(Long claimId, String result) {
        Claim claim = require(claimId);
        Item item = itemQueryService.require(claim.getItemId());
        validateDecision(claim, item);
        updateDecision(claim, result);

        if ("APPROVED".equals(result)) {
            approveClaim(item);
        }
        notifyApplicant(claim, item, result);
        return assembler.toView(claim);
    }

    private Claim require(Long id) {
        Claim claim = claimMapper.selectById(id);
        if (claim == null) throw BusinessException.notFound("认领申请不存在");
        return claim;
    }

    private void validateDecision(Claim claim, Item item) {
        if (!item.getUserId().equals(CurrentUser.get().id())) {
            throw BusinessException.forbidden("只有信息发布者可以处理认领申请");
        }
        if (!"PENDING".equals(claim.getStatus())) {
            throw BusinessException.badRequest("该申请已经处理");
        }
        if (!"PUBLISHED".equals(item.getStatus())) {
            throw BusinessException.badRequest("该物品当前不可处理认领");
        }
    }

    private void updateDecision(Claim claim, String result) {
        claim.setStatus(result);
        claim.setProcessedAt(LocalDateTime.now());
        claimMapper.updateById(claim);
    }

    private void approveClaim(Item item) {
        itemLifecycleService.markClaimed(item.getId());
        rejectOtherClaims(item);
    }

    private void rejectOtherClaims(Item item) {
        List<Claim> others = claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, item.getId()).eq(Claim::getStatus, "PENDING"));
        for (Claim other : others) {
            updateDecision(other, "REJECTED");
            notificationService.create(other.getApplicantId(), "CLAIM_RESULT",
                    "你对“" + item.getName() + "”的认领申请未通过");
        }
    }

    private void notifyApplicant(Claim claim, Item item, String result) {
        String resultText = "APPROVED".equals(result) ? "已通过" : "未通过";
        notificationService.create(claim.getApplicantId(), "CLAIM_RESULT",
                "你对“" + item.getName() + "”的认领申请" + resultText);
    }
}
