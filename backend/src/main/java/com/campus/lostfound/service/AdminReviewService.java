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

/**
 * 管理员的物品审核：看待审列表、通过或驳回、留下审核痕迹。
 *
 * <p>一次审核由三件事组成，本类负责串起来，每件事各自委托给专职协作者：
 * <ol>
 *   <li>改状态 —— {@link ItemLifecycleService#applyReview}（状态机规则归它管）</li>
 *   <li>留记录 —— 本类的 {@code saveReviewRecord}（谁在什么时候以什么理由审的，可追溯）</li>
 *   <li>发通知 —— {@link NotificationService}（把结果告知发布者）</li>
 * </ol>
 *
 * <p>三件事必须同生共死，因此 {@link #review} 标注 {@link Transactional}：
 * 不能出现"状态改了但没通知到"或"通知发了但状态没改"的中间态。
 */
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

    /**
     * 列出所有待审核的物品，按提交时间正序 —— 先到先审，避免早期提交被一直压在队尾。
     *
     * <p>注意返回的是已经过 {@code assembler} 转换的视图，每行都会附带查询分类与发布者，
     * 列表较长时存在明显的重复查询，属于当前实现的已知性能瓶颈。
     *
     * @return 待审核物品的只读视图列表，无数据时为空列表
     */
    public List<ItemDtos.View> pendingItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getStatus, "PENDING").orderByAsc(Item::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    /**
     * 审核一件物品：改状态、留记录、通知发布者，三步在同一事务内完成。
     *
     * @param itemId  物品主键
     * @param result  审核结论，取值 {@code APPROVED} / {@code REJECTED}
     * @param comment 审核意见，可空
     * @return 审核后物品的最新视图，前端可用它直接刷新列表行
     * @throws com.campus.lostfound.common.BusinessException 物品不存在，或当前状态不允许审核时抛出
     */
    @Transactional
    public ItemDtos.View review(Long itemId, String result, String comment) {
        Item item = itemLifecycleService.applyReview(itemId, result);
        saveReviewRecord(itemId, result, comment);
        notifyPublisher(item, result, comment);
        return assembler.toView(item);
    }

    /**
     * 落一条审核记录，用于事后追溯"这单是谁审的、依据什么"。
     *
     * <p>审核人取自当前登录身份而非请求参数 —— 客户端无法伪造审核人，这是审计的基本要求。
     *
     * @param comment 审核意见，空白会被归一为 {@code null}，避免库里混入空串与 null 两种"空"
     */
    private void saveReviewRecord(Long itemId, String result, String comment) {
        ReviewRecord record = new ReviewRecord();
        record.setItemId(itemId);
        record.setReviewerId(CurrentUser.get().id());
        record.setResult(result);
        record.setComment(trimToNull(comment));
        record.setReviewTime(LocalDateTime.now());
        reviewRecordMapper.insert(record);
    }

    /**
     * 把审核结果告知发布者。
     *
     * <p>通知正文由物品名 + 结论 + 意见拼成，让发布者不必点进详情就能看懂发生了什么。
     */
    private void notifyPublisher(Item item, String result, String comment) {
        String resultText = "APPROVED".equals(result) ? "审核通过" : "审核未通过";
        String normalizedComment = trimToNull(comment);
        String suffix = normalizedComment == null ? "" : "：" + normalizedComment;
        notificationService.create(item.getUserId(), "REVIEW",
                "“" + item.getName() + "”" + resultText + suffix);
    }

    /**
     * 归一化可选的自由文本：空白串与 {@code null} 统一为 {@code null}。
     *
     * <p>否则库里会同时存在 {@code ""} 和 {@code NULL} 两种"没填"，
     * 下游判断"有没有意见"就得多写一个条件。
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
