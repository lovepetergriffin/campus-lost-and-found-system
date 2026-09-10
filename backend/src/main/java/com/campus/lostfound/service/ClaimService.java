package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Claim;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimService {
    private final ClaimMapper claimMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final ItemService itemService;
    private final NotificationService notificationService;

    public ClaimService(ClaimMapper claimMapper, ItemMapper itemMapper, UserMapper userMapper,
                        ItemService itemService, NotificationService notificationService) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.itemService = itemService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ClaimDtos.View create(Long itemId, ClaimDtos.CreateRequest request) {
        Item item = itemService.require(itemId);
        Long applicantId = CurrentUser.get().id();
        if (!"PUBLISHED".equals(item.getStatus())) throw BusinessException.badRequest("该物品当前不可认领");
        if (item.getUserId().equals(applicantId)) throw BusinessException.badRequest("不能认领自己发布的物品");
        Long duplicate = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getItemId, itemId).eq(Claim::getApplicantId, applicantId));
        if (duplicate > 0) throw BusinessException.badRequest("你已提交过该物品的认领申请");

        Claim claim = new Claim();
        claim.setItemId(itemId);
        claim.setApplicantId(applicantId);
        claim.setDescription(request.description().trim());
        claim.setProof(request.proof().trim());
        claim.setStatus("PENDING");
        claim.setCreatedAt(LocalDateTime.now());
        claimMapper.insert(claim);
        notificationService.create(item.getUserId(), "CLAIM", "“" + item.getName() + "”收到新的认领申请");
        return toView(claim);
    }

    public List<ClaimDtos.View> myApplications() {
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                        .eq(Claim::getApplicantId, CurrentUser.get().id()).orderByDesc(Claim::getCreatedAt))
                .stream().map(this::toView).toList();
    }

    public List<ClaimDtos.View> received() {
        List<Long> itemIds = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, CurrentUser.get().id()))
                .stream().map(Item::getId).toList();
        if (itemIds.isEmpty()) return List.of();
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                        .in(Claim::getItemId, itemIds).orderByDesc(Claim::getCreatedAt))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public ClaimDtos.View decide(Long claimId, String result) {
        Claim claim = require(claimId);
        Item item = itemService.require(claim.getItemId());
        if (!item.getUserId().equals(CurrentUser.get().id())) {
            throw BusinessException.forbidden("只有信息发布者可以处理认领申请");
        }
        if (!"PENDING".equals(claim.getStatus())) throw BusinessException.badRequest("该申请已经处理");
        if (!"PUBLISHED".equals(item.getStatus())) throw BusinessException.badRequest("该物品当前不可处理认领");

        claim.setStatus(result);
        claim.setProcessedAt(LocalDateTime.now());
        claimMapper.updateById(claim);

        if ("APPROVED".equals(result)) {
            item.setStatus("CLAIMED");
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
            List<Claim> others = claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                    .eq(Claim::getItemId, item.getId()).eq(Claim::getStatus, "PENDING"));
            for (Claim other : others) {
                other.setStatus("REJECTED");
                other.setProcessedAt(LocalDateTime.now());
                claimMapper.updateById(other);
                notificationService.create(other.getApplicantId(), "CLAIM_RESULT",
                        "你对“" + item.getName() + "”的认领申请未通过");
            }
        }
        String resultText = "APPROVED".equals(result) ? "已通过" : "未通过";
        notificationService.create(claim.getApplicantId(), "CLAIM_RESULT",
                "你对“" + item.getName() + "”的认领申请" + resultText);
        return toView(claim);
    }

    public Claim require(Long id) {
        Claim claim = claimMapper.selectById(id);
        if (claim == null) throw BusinessException.notFound("认领申请不存在");
        return claim;
    }

    public ClaimDtos.View toView(Claim claim) {
        Item item = itemMapper.selectById(claim.getItemId());
        User applicant = userMapper.selectById(claim.getApplicantId());
        return new ClaimDtos.View(claim.getId(), claim.getItemId(), item == null ? "未知物品" : item.getName(),
                claim.getApplicantId(), applicant == null ? "未知用户" : applicant.getUsername(),
                claim.getDescription(), claim.getProof(), claim.getStatus(), claim.getCreatedAt(), claim.getProcessedAt());
    }
}
