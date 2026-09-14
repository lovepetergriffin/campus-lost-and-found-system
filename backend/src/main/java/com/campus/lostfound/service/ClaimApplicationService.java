package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Claim;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimApplicationService {
    private final ClaimMapper claimMapper;
    private final ItemMapper itemMapper;
    private final ItemQueryService itemQueryService;
    private final ClaimViewAssembler assembler;
    private final NotificationService notificationService;

    public ClaimApplicationService(ClaimMapper claimMapper, ItemMapper itemMapper,
                                   ItemQueryService itemQueryService, ClaimViewAssembler assembler,
                                   NotificationService notificationService) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.itemQueryService = itemQueryService;
        this.assembler = assembler;
        this.notificationService = notificationService;
    }

    @Transactional
    public ClaimDtos.View create(Long itemId, ClaimDtos.CreateRequest request) {
        Item item = itemQueryService.require(itemId);
        Long applicantId = CurrentUser.get().id();
        validateApplication(item, applicantId);

        Claim claim = new Claim();
        claim.setItemId(itemId);
        claim.setApplicantId(applicantId);
        claim.setDescription(request.description().trim());
        claim.setProof(request.proof().trim());
        claim.setStatus("PENDING");
        claim.setCreatedAt(LocalDateTime.now());
        claimMapper.insert(claim);

        notificationService.create(item.getUserId(), "CLAIM", "“" + item.getName() + "”收到新的认领申请");
        return assembler.toView(claim);
    }

    public List<ClaimDtos.View> myApplications() {
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                        .eq(Claim::getApplicantId, CurrentUser.get().id()).orderByDesc(Claim::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    public List<ClaimDtos.View> received() {
        List<Long> itemIds = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, CurrentUser.get().id()))
                .stream().map(Item::getId).toList();
        if (itemIds.isEmpty()) return List.of();
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                        .in(Claim::getItemId, itemIds).orderByDesc(Claim::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    private void validateApplication(Item item, Long applicantId) {
        if (!"PUBLISHED".equals(item.getStatus())) {
            throw BusinessException.badRequest("该物品当前不可认领");
        }
        if (item.getUserId().equals(applicantId)) {
            throw BusinessException.badRequest("不能认领自己发布的物品");
        }
        Long duplicate = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, item.getId()).eq(Claim::getApplicantId, applicantId));
        if (duplicate > 0) {
            throw BusinessException.badRequest("你已提交过该物品的认领申请");
        }
    }
}
